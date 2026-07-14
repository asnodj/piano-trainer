package dev.asnodj.pianotrainer.midi

/**
 * Connection status of the physical MIDI keyboard, exposed to the UI.
 */
sealed interface MidiConnectionState {
    /** No MIDI device with an output port is plugged in. */
    data object Disconnected : MidiConnectionState

    /** A device was detected and is being opened. */
    data class Connecting(val deviceName: String) : MidiConnectionState

    /** The device is open and note events are flowing. */
    data class Connected(val deviceName: String) : MidiConnectionState
}
