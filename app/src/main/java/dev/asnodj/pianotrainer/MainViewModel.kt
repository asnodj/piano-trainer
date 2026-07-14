package dev.asnodj.pianotrainer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.asnodj.pianotrainer.audio.SongPlayer
import dev.asnodj.pianotrainer.lesson.HandMode
import dev.asnodj.pianotrainer.lesson.LessonEngine
import dev.asnodj.pianotrainer.lesson.TempoEngine
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.midi.MidiInputManager
import dev.asnodj.pianotrainer.profile.PROFILES
import dev.asnodj.pianotrainer.profile.ProfileRepository
import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongNote
import dev.asnodj.pianotrainer.song.SongRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.TimeSource

/** Screens of the app (state-based navigation, no nav library needed yet). */
sealed interface Screen {
    data object Home : Screen
    data object Discovery : Screen
    data class Lesson(val song: Song) : Screen
}

/** Available tempo multipliers for playback and wait-mode scrolling. */
val SPEED_OPTIONS = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)

/** The two ways of practicing a song. */
enum class PracticeMode {
    /** Song frozen until the correct key (the default learning state). */
    WAIT,

    /** Song scrolls on its own; notes are judged in timing windows. */
    TEMPO,
}

/**
 * App-level state holder: owns the MIDI input manager, the bundled songs, the
 * demo player and the lesson engine of the current lesson, and routes between
 * screens.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val midiInputManager = MidiInputManager(application)
    private val songRepository = SongRepository(application)
    private val songPlayer = SongPlayer(viewModelScope)
    private val profileRepository = ProfileRepository(application)

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

    private val mutablePracticeMode = MutableStateFlow(PracticeMode.WAIT)

    /** Current practice mode of the lesson. */
    val practiceMode: StateFlow<PracticeMode> = mutablePracticeMode.asStateFlow()

    private val mutableTempoEngine = MutableStateFlow<TempoEngine?>(null)

    /** Engine of the tempo run in progress, null in wait mode. */
    val tempoEngine: StateFlow<TempoEngine?> = mutableTempoEngine.asStateFlow()

    private val mutableSemiAutoEnabled = MutableStateFlow(false)

    /** True when the app plays the other hand as accompaniment (semi-auto). */
    val semiAutoEnabled: StateFlow<Boolean> = mutableSemiAutoEnabled.asStateFlow()

    /** Id of the active family profile. */
    val selectedProfileId: StateFlow<String> = profileRepository.selectedProfileId
        .stateIn(viewModelScope, SharingStarted.Eagerly, PROFILES.first().id)

    /** Best scores of the active profile, keyed by "songId/mode". */
    @OptIn(ExperimentalCoroutinesApi::class)
    val bestScores: StateFlow<Map<String, Int>> = profileRepository.selectedProfileId
        .flatMapLatest { profileId -> profileRepository.bestScores(profileId) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    private var lessonInputJob: Job? = null
    private var accompanimentJob: Job? = null
    private var scoreSaveJob: Job? = null
    private var tempoClockJob: Job? = null

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
     * Switches the active family profile.
     *
     * @param profileId One of the [PROFILES] ids.
     */
    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            profileRepository.selectProfile(profileId)
        }
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

    /** Restarts the current lesson from the beginning (in the active mode). */
    fun restartLesson() {
        val currentScreen = mutableScreen.value
        if (currentScreen is Screen.Lesson) {
            if (mutablePracticeMode.value == PracticeMode.TEMPO) {
                startTempoRun(currentScreen.song)
            } else {
                startEngine(currentScreen.song)
            }
        }
    }

    /** Toggles the semi-auto accompaniment (app plays the other hand). */
    fun toggleSemiAuto() {
        mutableSemiAutoEnabled.value = !mutableSemiAutoEnabled.value
    }

    /**
     * Switches between wait mode and tempo mode. Entering tempo mode starts a
     * run from the beginning of the song; leaving it returns to wait mode at
     * the start.
     */
    fun togglePracticeMode() {
        val currentScreen = mutableScreen.value
        if (currentScreen !is Screen.Lesson) {
            return
        }
        if (mutablePracticeMode.value == PracticeMode.WAIT) {
            mutablePracticeMode.value = PracticeMode.TEMPO
            songPlayer.stop()
            startTempoRun(currentScreen.song)
        } else {
            stopTempoRun()
            mutablePracticeMode.value = PracticeMode.WAIT
            mutableLessonEngine.value?.seekToGroup(0)
        }
    }

    /**
     * Starts (or restarts) a tempo run: fresh engine plus the 60 Hz clock that
     * drives it and the semi-auto accompaniment.
     *
     * @param song Song to run.
     */
    private fun startTempoRun(song: Song) {
        songPlayer.stop()
        tempoClockJob?.cancel()
        val engine = TempoEngine(song, mutableHandMode.value)
        mutableTempoEngine.value = engine
        val accompaniment = accompanimentNotesOf(song)
        tempoClockJob = viewModelScope.launch {
            val runStart = TimeSource.Monotonic.markNow()
            var previousElapsedMs = 0L
            var songPositionMs = 0.0
            var accompanimentFromMs = 0
            while (isActive && !engine.state.value.finished) {
                val elapsedMs = runStart.elapsedNow().inWholeMilliseconds
                songPositionMs += (elapsedMs - previousElapsedMs) * mutableSpeedFactor.value
                previousElapsedMs = elapsedMs
                val positionMs = songPositionMs.toInt()
                if (mutableSemiAutoEnabled.value) {
                    songPlayer.playAccompaniment(
                        notes = accompaniment.filter { songNote ->
                            songNote.startMs >= accompanimentFromMs && songNote.startMs < positionMs
                        },
                        windowStartMs = accompanimentFromMs,
                        speedFactor = mutableSpeedFactor.value,
                    )
                }
                accompanimentFromMs = positionMs
                engine.advanceTo(positionMs)
                delay(16)
            }
            if (engine.state.value.finished) {
                profileRepository.saveBestScore(
                    profileId = selectedProfileId.value,
                    songId = song.id,
                    mode = "tempo",
                    score = engine.state.value.scorePercent,
                )
            }
        }
    }

    /** Stops the tempo clock and clears the tempo engine. */
    private fun stopTempoRun() {
        tempoClockJob?.cancel()
        tempoClockJob = null
        mutableTempoEngine.value = null
        songPlayer.stop()
    }

    /**
     * Notes of the hand(s) the player is NOT practicing (the accompaniment).
     *
     * @param song Current song.
     * @return The other hand's notes; empty when practicing both hands.
     */
    private fun accompanimentNotesOf(song: Song): List<SongNote> {
        return when (mutableHandMode.value) {
            HandMode.BOTH -> emptyList()
            HandMode.RIGHT -> song.notes.filter { songNote -> songNote.hand == Hand.LEFT }
            HandMode.LEFT -> song.notes.filter { songNote -> songNote.hand == Hand.RIGHT }
        }
    }

    /**
     * Routes a debug-build virtual-keyboard touch into the MIDI stream, and
     * sounds it through the phone synth (no physical instrument is producing
     * the sound in touch mode).
     *
     * @param note MIDI note number.
     * @param isNoteOn True on press, false on release.
     */
    fun injectTouchNote(note: Int, isNoteOn: Boolean) {
        midiInputManager.injectNoteEvent(note, isNoteOn)
        songPlayer.playLiveNote(note, isNoteOn)
    }

    /** Returns to the home screen and stops the current lesson if any. */
    fun goHome() {
        stopTempoRun()
        mutablePracticeMode.value = PracticeMode.WAIT
        songPlayer.stop()
        lessonInputJob?.cancel()
        lessonInputJob = null
        accompanimentJob?.cancel()
        accompanimentJob = null
        mutableLessonEngine.value = null
        mutableScreen.value = Screen.Home
    }

    /**
     * Creates a fresh wait-mode engine for the song, plugs the MIDI stream
     * into the active engine (wait or tempo), and wires the semi-auto
     * accompaniment to the wait-mode position. Keyboard input is ignored
     * while the demo playback is running.
     *
     * @param song Song to practice.
     */
    private fun startEngine(song: Song) {
        lessonInputJob?.cancel()
        accompanimentJob?.cancel()
        scoreSaveJob?.cancel()
        val engine = LessonEngine(song, mutableHandMode.value)
        mutableLessonEngine.value = engine
        // Persist the best accuracy when the wait-mode run completes.
        scoreSaveJob = viewModelScope.launch {
            engine.state.map { lessonState -> lessonState.finished }
                .distinctUntilChanged()
                .collect { finished ->
                    if (finished) {
                        profileRepository.saveBestScore(
                            profileId = selectedProfileId.value,
                            songId = song.id,
                            mode = "wait",
                            score = engine.state.value.accuracyPercent,
                        )
                    }
                }
        }
        lessonInputJob = viewModelScope.launch {
            midiInputManager.noteEvents.collect { noteEvent ->
                if (songPlayer.isPlaying.value) {
                    return@collect
                }
                val tempoEngineNow = mutableTempoEngine.value
                if (mutablePracticeMode.value == PracticeMode.TEMPO && tempoEngineNow != null) {
                    if (noteEvent.isNoteOn) {
                        tempoEngineNow.onNoteOn(noteEvent.note)
                    }
                } else {
                    if (noteEvent.isNoteOn) {
                        engine.onNoteOn(noteEvent.note)
                    } else {
                        engine.onNoteOff(noteEvent.note)
                    }
                }
            }
        }
        // Semi-auto in wait mode: when the player advances, the accompaniment
        // notes crossed by the move are played at their tempo-scaled offsets.
        val accompaniment = accompanimentNotesOf(song)
        accompanimentJob = viewModelScope.launch {
            var previousPositionMs = engine.state.value.positionMs
            engine.state.map { lessonState -> lessonState.positionMs }
                .distinctUntilChanged()
                .collect { newPositionMs ->
                    if (newPositionMs > previousPositionMs &&
                        mutableSemiAutoEnabled.value &&
                        mutablePracticeMode.value == PracticeMode.WAIT &&
                        !songPlayer.isPlaying.value
                    ) {
                        songPlayer.playAccompaniment(
                            notes = accompaniment.filter { songNote ->
                                songNote.startMs >= previousPositionMs && songNote.startMs < newPositionMs
                            },
                            windowStartMs = previousPositionMs,
                            speedFactor = mutableSpeedFactor.value,
                        )
                    }
                    previousPositionMs = newPositionMs
                }
        }
    }

    override fun onCleared() {
        songPlayer.release()
        midiInputManager.stop()
    }
}
