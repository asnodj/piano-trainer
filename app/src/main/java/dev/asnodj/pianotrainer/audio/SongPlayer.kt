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
import kotlin.time.TimeSource

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

    @Volatile
    private var currentSpeedFactor: Float = 1.0f

    private val mutableIsPlaying = MutableStateFlow(false)

    /** True while the song is being played back. */
    val isPlaying: StateFlow<Boolean> = mutableIsPlaying.asStateFlow()

    private val mutablePositionMs = MutableStateFlow(0)

    /** Current playback position in song milliseconds. */
    val positionMs: StateFlow<Int> = mutablePositionMs.asStateFlow()

    /** One scheduled MIDI event of the playback. */
    private data class PlaybackEvent(val atMs: Int, val note: Int, val isNoteOn: Boolean)

    /**
     * Starts playing the given notes from the beginning, stopping any playback
     * in progress. Everything is driven by a monotonic wall clock: the position
     * flow advances continuously (~60 Hz) so the note highway scrolls smoothly
     * through sustained notes, and MIDI events fire off the same clock so the
     * audio can never drift from the display.
     *
     * @param notes Song notes sorted by start time.
     * @param speedFactor Initial tempo multiplier (adjustable live via [setSpeed]).
     * @param onFinished Called when the song reaches its end.
     */
    fun play(notes: List<SongNote>, speedFactor: Float, onFinished: () -> Unit = {}) {
        stop()
        currentSpeedFactor = speedFactor
        midiDriver.start()
        mutableIsPlaying.value = true
        playbackJob = scope.launch {
            try {
                val events = buildList {
                    notes.forEach { songNote ->
                        add(PlaybackEvent(songNote.startMs, songNote.midiNote, isNoteOn = true))
                        add(PlaybackEvent(songNote.startMs + songNote.durationMs, songNote.midiNote, isNoteOn = false))
                    }
                }.sortedWith(compareBy({ event -> event.atMs }, { event -> event.isNoteOn }))

                val playbackStart = TimeSource.Monotonic.markNow()
                // Song position is integrated tick by tick so the speed factor
                // can change mid-playback without making the position jump.
                var songPositionMs = 0.0
                var previousElapsedMs = 0L
                var nextEventIndex = 0
                while (nextEventIndex < events.size) {
                    val elapsedMs = playbackStart.elapsedNow().inWholeMilliseconds
                    songPositionMs += (elapsedMs - previousElapsedMs) * currentSpeedFactor
                    previousElapsedMs = elapsedMs
                    while (nextEventIndex < events.size && events[nextEventIndex].atMs <= songPositionMs) {
                        val event = events[nextEventIndex]
                        if (event.isNoteOn) {
                            sendNoteOn(event.note)
                        } else {
                            sendNoteOff(event.note)
                        }
                        nextEventIndex++
                    }
                    mutablePositionMs.value = songPositionMs.toInt()
                    delay(16)
                }
            } finally {
                allNotesOff()
                mutableIsPlaying.value = false
            }
            onFinished()
        }
    }

    /**
     * Changes the tempo multiplier, effective immediately even mid-playback.
     *
     * @param speedFactor New tempo multiplier.
     */
    fun setSpeed(speedFactor: Float) {
        currentSpeedFactor = speedFactor
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
