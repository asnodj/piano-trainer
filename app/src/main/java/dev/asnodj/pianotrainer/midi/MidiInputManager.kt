package dev.asnodj.pianotrainer.midi

import android.content.Context
import android.media.midi.MidiDevice
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiManager
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Connects to the first USB MIDI keyboard that appears (hot-plug aware) and
 * exposes its note stream and connection status as flows.
 *
 * Terminology trap: to RECEIVE notes from the keyboard we open the device's
 * OUTPUT port (data flows out of the device into the app).
 */
class MidiInputManager(context: Context) {

    private val midiManager = context.getSystemService(Context.MIDI_SERVICE) as MidiManager
    private val mainThreadHandler = Handler(Looper.getMainLooper())
    private val parser = MidiMessageParser()

    private val mutableConnectionState =
        MutableStateFlow<MidiConnectionState>(MidiConnectionState.Disconnected)

    /** Current keyboard connection status, for the UI. */
    val connectionState: StateFlow<MidiConnectionState> = mutableConnectionState.asStateFlow()

    private val mutableNoteEvents = MutableSharedFlow<MidiNoteEvent>(extraBufferCapacity = 256)

    /** Live stream of note-on/note-off events from the keyboard. */
    val noteEvents: SharedFlow<MidiNoteEvent> = mutableNoteEvents.asSharedFlow()

    private var openedDevice: MidiDevice? = null
    private var openedOutputPort: MidiOutputPort? = null
    private var connectedDeviceId: Int? = null

    private val noteReceiver = object : MidiReceiver() {
        override fun onSend(message: ByteArray, offset: Int, count: Int, timestamp: Long) {
            parser.parse(message, offset, count, timestamp).forEach { noteEvent ->
                mutableNoteEvents.tryEmit(noteEvent)
            }
        }
    }

    private val deviceCallback = object : MidiManager.DeviceCallback() {
        override fun onDeviceAdded(deviceInfo: MidiDeviceInfo) {
            if (connectedDeviceId == null) {
                connectTo(deviceInfo)
            }
        }

        override fun onDeviceRemoved(deviceInfo: MidiDeviceInfo) {
            if (deviceInfo.id == connectedDeviceId) {
                closeCurrentDevice()
                mutableConnectionState.value = MidiConnectionState.Disconnected
            }
        }
    }

    /**
     * Starts watching for MIDI devices and connects to one if already plugged in.
     * Call once (e.g. from the ViewModel init); pair with [stop].
     */
    fun start() {
        @Suppress("DEPRECATION")
        midiManager.registerDeviceCallback(deviceCallback, mainThreadHandler)
        @Suppress("DEPRECATION")
        midiManager.devices.firstOrNull { it.outputPortCount > 0 }?.let(::connectTo)
    }

    /**
     * Injects a synthetic note event into the same stream as the hardware
     * keyboard. Development tool (debug builds tap the virtual keyboard):
     * lets the app be exercised end to end without the piano.
     *
     * @param note MIDI note number.
     * @param isNoteOn True for press, false for release.
     */
    fun injectNoteEvent(note: Int, isNoteOn: Boolean) {
        mutableNoteEvents.tryEmit(
            MidiNoteEvent(
                note = note,
                velocity = if (isNoteOn) 100 else 0,
                isNoteOn = isNoteOn,
                channel = 0,
                timestampNanos = System.nanoTime(),
            )
        )
    }

    /**
     * Stops watching for devices and releases the open port/device.
     */
    fun stop() {
        midiManager.unregisterDeviceCallback(deviceCallback)
        closeCurrentDevice()
        mutableConnectionState.value = MidiConnectionState.Disconnected
    }

    /**
     * Opens the given device and connects our receiver to its first output port.
     *
     * @param deviceInfo Device to connect to; ignored if it has no output port.
     */
    private fun connectTo(deviceInfo: MidiDeviceInfo) {
        if (deviceInfo.outputPortCount == 0) {
            return
        }
        val deviceName = readableNameOf(deviceInfo)
        mutableConnectionState.value = MidiConnectionState.Connecting(deviceName)
        midiManager.openDevice(deviceInfo, { device ->
            if (device == null) {
                mutableConnectionState.value = MidiConnectionState.Disconnected
                return@openDevice
            }
            val outputPort = device.openOutputPort(0)
            if (outputPort == null) {
                device.close()
                mutableConnectionState.value = MidiConnectionState.Disconnected
                return@openDevice
            }
            outputPort.connect(noteReceiver)
            openedDevice = device
            openedOutputPort = outputPort
            connectedDeviceId = deviceInfo.id
            mutableConnectionState.value = MidiConnectionState.Connected(deviceName)
        }, mainThreadHandler)
    }

    /**
     * Disconnects the receiver and closes the port and device, ignoring
     * close-time IO errors (the device may already be gone).
     */
    private fun closeCurrentDevice() {
        try {
            openedOutputPort?.disconnect(noteReceiver)
            openedOutputPort?.close()
            openedDevice?.close()
        } catch (_: java.io.IOException) {
            // Device already unplugged; nothing to release.
        }
        openedOutputPort = null
        openedDevice = null
        connectedDeviceId = null
    }

    /**
     * Returns a human-readable device name for the UI.
     *
     * @param deviceInfo Device to name.
     * @return Product name if the device advertises one, otherwise a generic label.
     */
    private fun readableNameOf(deviceInfo: MidiDeviceInfo): String {
        return deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_PRODUCT)
            ?: deviceInfo.properties.getString(MidiDeviceInfo.PROPERTY_NAME)
            ?: "Clavier MIDI"
    }
}
