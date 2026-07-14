package dev.asnodj.pianotrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.ui.DiscoveryScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                AppRoot()
            }
        }
    }
}

/**
 * Root screen: routes between the "plug in your keyboard" placeholder and the
 * discovery mode depending on the MIDI connection state.
 */
@Composable
fun AppRoot(viewModel: MainViewModel = viewModel()) {
    val connectionState by viewModel.connectionState.collectAsState()
    val pressedNotes by viewModel.pressedNotes.collectAsState()

    Surface(modifier = Modifier.fillMaxSize()) {
        when (val state = connectionState) {
            is MidiConnectionState.Connected ->
                DiscoveryScreen(deviceName = state.deviceName, pressedNotes = pressedNotes)

            is MidiConnectionState.Connecting ->
                CenteredMessage(text = stringResource(R.string.keyboard_connecting))

            MidiConnectionState.Disconnected ->
                CenteredMessage(text = stringResource(R.string.keyboard_disconnected))
        }
    }
}

/**
 * Full-screen centered status message with the app title above.
 *
 * @param text Status line to display.
 */
@Composable
private fun CenteredMessage(text: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
