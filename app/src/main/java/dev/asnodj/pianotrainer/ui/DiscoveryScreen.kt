package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.asnodj.pianotrainer.R

/**
 * Discovery mode: shows the virtual keyboard mirroring what is played on the
 * physical keyboard in real time, with golden sparkles per key press. The
 * sparkle overlay sits flush against the keyboard (same full width, no gap)
 * so the lights rise exactly from the pressed keys.
 *
 * @param deviceName Name of the connected MIDI keyboard, shown in the header.
 * @param pressedNotes MIDI note numbers currently held down.
 * @param onDebugTouch Debug-build note injection via the virtual keyboard.
 */
@Composable
fun DiscoveryScreen(
    deviceName: String,
    pressedNotes: Set<Int>,
    onDebugTouch: ((note: Int, isNoteOn: Boolean) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.keyboard_connected, deviceName),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
        ) {
            Text(
                text = stringResource(R.string.discovery_hint),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.align(Alignment.Center),
            )
            KeySparkles(
                pressedNotes = pressedNotes,
                colorFor = { PianoPalette.expectedHalo },
                modifier = Modifier.fillMaxSize(),
            )
        }
        PianoKeyboard(
            pressedNotes = pressedNotes,
            onDebugTouch = onDebugTouch,
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.45f),
        )
    }
}
