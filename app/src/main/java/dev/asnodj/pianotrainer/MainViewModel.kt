package dev.asnodj.pianotrainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.asnodj.pianotrainer.audio.SongPlayer
import dev.asnodj.pianotrainer.lesson.HandMode
import dev.asnodj.pianotrainer.lesson.LessonEngine
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.midi.MidiInputManager
import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Screens of the app (state-based navigation, no nav library needed yet). */
sealed interface Screen {
    data object Home : Screen
    data object Discovery : Screen
    data class Lesson(val song: Song) : Screen
}

/** Available tempo multipliers for playback and wait-mode scrolling. */
val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

/**
 * App-level state holder: owns the MIDI input manager, the bundled songs, the
 * demo player and the lesson engine of the current lesson, and routes between
 * screens.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val midiInputManager = MidiInputManager(application)
    private val songRepository = SongRepository(application)
    private val songPlayer = SongPlayer(viewModelScope)

    /** Keyboard connection status for the UI. */
    val connectionState: StateFlow<MidiConnectionState> = midiInputManager.connectionState

    /** MIDI note numbers currently held down on the physical keyboard. */
    val pressedNotes: StateFlow<Set<Int>> = midiInputManager.noteEvents
        .scan(emptySet<Int>()) { currentlyPressed, noteEvent ->
            if (noteEvent.isNoteOn) {
                currentlyPressed + noteEvent.note
            } else {
                currentlyPressed - noteEvent.note
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    private val mutableSongs = MutableStateFlow<List<Song>>(emptyList())

    /** Bundled songs, easiest first. */
    val songs: StateFlow<List<Song>> = mutableSongs.asStateFlow()

    private val mutableScreen = MutableStateFlow<Screen>(Screen.Home)

    /** Currently displayed screen. */
    val screen: StateFlow<Screen> = mutableScreen.asStateFlow()

    private val mutableLessonEngine = MutableStateFlow<LessonEngine?>(null)

    /** Engine of the lesson in progress, null outside lessons. */
    val lessonEngine: StateFlow<LessonEngine?> = mutableLessonEngine.asStateFlow()

    private val mutableHandMode = MutableStateFlow(HandMode.RIGHT)

    /** Hand(s) currently practiced in the lesson. */
    val handMode: StateFlow<HandMode> = mutableHandMode.asStateFlow()

    private val mutableSpeedFactor = MutableStateFlow(1.0f)

    /** Tempo multiplier applied to playback and wait-mode scrolling. */
    val speedFactor: StateFlow<Float> = mutableSpeedFactor.asStateFlow()

    /** True while the demo playback is running. */
    val isPlaying: StateFlow<Boolean> = songPlayer.isPlaying

    /** Demo playback position, drives the highway while playing. */
    val playbackPositionMs: StateFlow<Int> = songPlayer.positionMs

    private var lessonInputJob: Job? = null

    init {
        midiInputManager.start()
        viewModelScope.launch(Dispatchers.IO) {
            mutableSongs.value = songRepository.loadBundledSongs()
        }
    }

    /** Opens the discovery screen. */
    fun openDiscovery() {
        mutableScreen.value = Screen.Discovery
    }

    /**
     * Opens a song in lesson mode with the current hand mode.
     *
     * @param song Song to practice.
     */
    fun openLesson(song: Song) {
        mutableScreen.value = Screen.Lesson(song)
        startEngine(song)
    }

    /**
     * Toggles one hand on/off. Both hands active means HandMode.BOTH; the last
     * active hand cannot be turned off. Restarts the lesson on change.
     *
     * @param hand Hand whose switch was tapped.
     */
    fun toggleHand(hand: Hand) {
        val current = mutableHandMode.value
        val next = when {
            current == HandMode.BOTH && hand == Hand.LEFT -> HandMode.RIGHT
            current == HandMode.BOTH && hand == Hand.RIGHT -> HandMode.LEFT
            current == HandMode.RIGHT && hand == Hand.LEFT -> HandMode.BOTH
            current == HandMode.LEFT && hand == Hand.RIGHT -> HandMode.BOTH
            else -> current // Turning off the last active hand: refused.
        }
        if (next != current) {
            mutableHandMode.value = next
            val currentScreen = mutableScreen.value
            if (currentScreen is Screen.Lesson) {
                // Stay where the player was in the song instead of restarting.
                val previousPositionMs = mutableLessonEngine.value?.state?.value?.positionMs ?: 0
                startEngine(currentScreen.song)
                mutableLessonEngine.value?.seekToMs(previousPositionMs)
            }
        }
    }

    /**
     * Jumps inside the current lesson (seek bar / rewind button). Stops the
     * demo playback so the wait mode takes over at the new position.
     *
     * @param fraction 0.0 = beginning, 1.0 = end of the song.
     */
    fun seekTo(fraction: Float) {
        songPlayer.stop()
        mutableLessonEngine.value?.seekToFraction(fraction)
    }

    /**
     * Changes the tempo multiplier.
     *
     * @param factor One of [SPEED_OPTIONS].
     */
    fun changeSpeed(factor: Float) {
        mutableSpeedFactor.value = factor
        songPlayer.setSpeed(factor)
    }

    /**
     * Starts or stops the demo playback of the current lesson's song. Only the
     * selected hand(s) are played, so the "listen" step matches exactly what
     * the player is about to practice.
     */
    fun togglePlayback() {
        if (songPlayer.isPlaying.value) {
            songPlayer.stop()
            return
        }
        val currentScreen = mutableScreen.value
        if (currentScreen is Screen.Lesson) {
            val notesToPlay = when (mutableHandMode.value) {
                HandMode.BOTH -> currentScreen.song.notes
                HandMode.RIGHT -> currentScreen.song.notes.filter { songNote -> songNote.hand == Hand.RIGHT }
                HandMode.LEFT -> currentScreen.song.notes.filter { songNote -> songNote.hand == Hand.LEFT }
            }
            songPlayer.play(notesToPlay, mutableSpeedFactor.value)
        }
    }

    /** Restarts the current lesson from the beginning. */
    fun restartLesson() {
        val currentScreen = mutableScreen.value
        if (currentScreen is Screen.Lesson) {
            startEngine(currentScreen.song)
        }
    }

    /**
     * Routes a debug-build virtual-keyboard touch into the MIDI stream.
     *
     * @param note MIDI note number.
     * @param isNoteOn True on press, false on release.
     */
    fun injectTouchNote(note: Int, isNoteOn: Boolean) {
        midiInputManager.injectNoteEvent(note, isNoteOn)
    }

    /** Returns to the home screen and stops the current lesson if any. */
    fun goHome() {
        songPlayer.stop()
        lessonInputJob?.cancel()
        lessonInputJob = null
        mutableLessonEngine.value = null
        mutableScreen.value = Screen.Home
    }

    /**
     * Creates a fresh engine for the song and plugs the MIDI stream into it.
     * Keyboard input is ignored while the demo playback is running.
     *
     * @param song Song to practice.
     */
    private fun startEngine(song: Song) {
        lessonInputJob?.cancel()
        val engine = LessonEngine(song, mutableHandMode.value)
        mutableLessonEngine.value = engine
        lessonInputJob = viewModelScope.launch {
            midiInputManager.noteEvents.collect { noteEvent ->
                if (songPlayer.isPlaying.value) {
                    return@collect
                }
                if (noteEvent.isNoteOn) {
                    engine.onNoteOn(noteEvent.note)
                } else {
                    engine.onNoteOff(noteEvent.note)
                }
            }
        }
    }

    override fun onCleared() {
        songPlayer.release()
        midiInputManager.stop()
    }
}
