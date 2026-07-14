package dev.asnodj.pianotrainer.audio

import dev.asnodj.pianotrainer.song.SongNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.billthefarmer.mididriver.MidiDriver

/**
 * Plays a song through the built-in Sonivox synthesizer (mididriver) for the
 * "listen" step of the practice ladder. Exposes the playback position so the
 * note highway can scroll along.
 *
 * @param scope Scope owning the playback coroutine (the ViewModel scope).
 */
class SongPlayer(private val scope: CoroutineScope) {

    private val midiDriver: MidiDriver = MidiDriver.getInstance()
    private var playbackJob: Job? = null

    private val mutableIsPlaying = MutableStateFlow(false)

    /** True while the song is being played back. */
    val isPlaying: StateFlow<Boolean> = mutableIsPlaying.asStateFlow()

    private val mutablePositionMs = MutableStateFlow(0)

    /** Current playback position in song milliseconds. */
    val positionMs: StateFlow<Int> = mutablePositionMs.asStateFlow()

    /**
     * Starts playing the given notes from the beginning, stopping any playback
     * in progress.
     *
     * @param notes Song notes sorted by start time.
     * @param speedFactor Tempo multiplier (1.0 = authored tempo, 0.5 = half speed).
     * @param onFinished Called on the main thread when the song reaches its end.
     */
    fun play(notes: List<SongNote>, speedFactor: Float, onFinished: () -> Unit = {}) {
        stop()
        midiDriver.start()
        mutableIsPlaying.value = true
        playbackJob = scope.launch {
            try {
                var currentMs = 0
                val pendingOffs = mutableListOf<Pair<Int, Int>>() // (offAtMs, note)
                notes.forEach { songNote ->
                    // Release every note ending before this one starts.
                    pendingOffs.sortBy { (offAtMs, _) -> offAtMs }
                    while (pendingOffs.isNotEmpty() && pendingOffs.first().first <= songNote.startMs) {
                        val (offAtMs, note) = pendingOffs.removeAt(0)
                        delayScaled(offAtMs - currentMs, speedFactor)
                        currentMs = offAtMs
                        sendNoteOff(note)
                        mutablePositionMs.value = currentMs
                    }
                    delayScaled(songNote.startMs - currentMs, speedFactor)
                    currentMs = songNote.startMs
                    sendNoteOn(songNote.midiNote)
                    mutablePositionMs.value = currentMs
                    pendingOffs.add((songNote.startMs + songNote.durationMs) to songNote.midiNote)
                }
                pendingOffs.sortBy { (offAtMs, _) -> offAtMs }
                pendingOffs.forEach { (offAtMs, note) ->
                    delayScaled(offAtMs - currentMs, speedFactor)
                    currentMs = offAtMs
                    sendNoteOff(note)
                    mutablePositionMs.value = currentMs
                }
            } finally {
                allNotesOff()
                mutableIsPlaying.value = false
            }
            onFinished()
        }
    }

    /** Stops the playback and silences the synthesizer. */
    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        allNotesOff()
        mutableIsPlaying.value = false
        mutablePositionMs.value = 0
    }

    /** Releases the synthesizer; call when the owner is destroyed. */
    fun release() {
        stop()
        midiDriver.stop()
    }

    /**
     * Waits the given song duration adjusted by the tempo multiplier.
     *
     * @param songMs Duration in song milliseconds (may be zero or negative).
     * @param speedFactor Tempo multiplier.
     */
    private suspend fun delayScaled(songMs: Int, speedFactor: Float) {
        if (songMs > 0) {
            delay((songMs / speedFactor).toLong())
        }
    }

    private fun sendNoteOn(note: Int) {
        midiDriver.write(byteArrayOf(0x90.toByte(), note.toByte(), 96))
    }

    private fun sendNoteOff(note: Int) {
        midiDriver.write(byteArrayOf(0x80.toByte(), note.toByte(), 0))
    }

    /** Sends "all notes off" on channel 0 so nothing keeps ringing. */
    private fun allNotesOff() {
        midiDriver.write(byteArrayOf(0xB0.toByte(), 123, 0))
    }
}
