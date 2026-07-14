package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.layout.Arrangement
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
 * physical keyboard in real time.
 *
 * @param deviceName Name of the connected MIDI keyboard, shown in the header.
 * @param pressedNotes MIDI note numbers currently held down.
 */
@Composable
fun DiscoveryScreen(deviceName: String, pressedNotes: Set<Int>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.keyboard_connected, deviceName),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.discovery_hint),
            style = MaterialTheme.typography.bodyLarge,
        )
        PianoKeyboard(
            pressedNotes = pressedNotes,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
    }
}
