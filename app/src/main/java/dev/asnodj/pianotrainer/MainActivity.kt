package dev.asnodj.pianotrainer

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.asnodj.pianotrainer.lesson.HandMode
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.ui.DiscoveryScreen
import dev.asnodj.pianotrainer.ui.HomeScreen
import dev.asnodj.pianotrainer.ui.LessonScreen
import dev.asnodj.pianotrainer.ui.PianoTrainerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Practice app: the player's hands are on the piano, not on the screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent {
            PianoTrainerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot()
                }
            }
        }
    }
}

/**
 * Root of the app: renders the current screen from the ViewModel and wires
 * the system back gesture to the in-app navigation.
 */
@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val screen by viewModel.screen.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val songs by viewModel.songs.collectAsState()
    val pressedNotes by viewModel.pressedNotes.collectAsState()
    val handMode by viewModel.handMode.collectAsState()
    val lessonEngine by viewModel.lessonEngine.collectAsState()
    val speedFactor by viewModel.speedFactor.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playbackPositionMs by viewModel.playbackPositionMs.collectAsState()
    val practiceMode by viewModel.practiceMode.collectAsState()
    val tempoEngine by viewModel.tempoEngine.collectAsState()
    val semiAutoEnabled by viewModel.semiAutoEnabled.collectAsState()

    BackHandler(enabled = screen != Screen.Home) {
        viewModel.goHome()
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.safeDrawingPadding()) {
        when (val currentScreen = screen) {
            Screen.Home -> HomeScreen(
                connectionState = connectionState,
                songs = songs,
                onOpenDiscovery = viewModel::openDiscovery,
                onOpenSong = viewModel::openLesson,
            )

            Screen.Discovery -> DiscoveryScreen(
                deviceName = (connectionState as? MidiConnectionState.Connected)?.deviceName ?: "",
                pressedNotes = pressedNotes,
                onDebugTouch = if (BuildConfig.DEBUG) viewModel::injectTouchNote else null,
            )

            is Screen.Lesson -> {
                val engine = lessonEngine
                if (engine != null) {
                    val lessonState by engine.state.collectAsState()
                    val availableHands = remember(currentScreen.song) {
                        currentScreen.song.notes.map { songNote -> songNote.hand }.toSet()
                    }
                    val currentTempoEngine = tempoEngine
                    val tempoState = currentTempoEngine?.state?.collectAsState()?.value
                    val accompanimentHandAvailable = when (handMode) {
                        HandMode.RIGHT -> Hand.LEFT in availableHands
                        HandMode.LEFT -> Hand.RIGHT in availableHands
                        HandMode.BOTH -> false
                    }
                    LessonScreen(
                        songTitle = currentScreen.song.title,
                        lessonState = lessonState,
                        physicalPressedNotes = pressedNotes,
                        noteGroups = engine.noteGroups,
                        handMode = handMode,
                        leftHandAvailable = Hand.LEFT in availableHands,
                        rightHandAvailable = Hand.RIGHT in availableHands,
                        speedFactor = speedFactor,
                        isPlaying = isPlaying,
                        playbackPositionMs = playbackPositionMs,
                        practiceMode = practiceMode,
                        tempoState = tempoState,
                        tempoNotes = currentTempoEngine?.notes ?: emptyList(),
                        semiAutoEnabled = semiAutoEnabled,
                        semiAutoAvailable = accompanimentHandAvailable,
                        onToggleHand = viewModel::toggleHand,
                        onSpeedChange = viewModel::changeSpeed,
                        onTogglePlayback = viewModel::togglePlayback,
                        onToggleSemiAuto = viewModel::toggleSemiAuto,
                        onTogglePracticeMode = viewModel::togglePracticeMode,
                        onSeek = viewModel::seekTo,
                        onRestart = viewModel::restartLesson,
                        onBack = viewModel::goHome,
                        onDebugTouch = if (BuildConfig.DEBUG) viewModel::injectTouchNote else null,
                    )
                }
            }
        }
    }
}
