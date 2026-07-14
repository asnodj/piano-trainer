package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Usb
import dev.asnodj.pianotrainer.R
import dev.asnodj.pianotrainer.midi.MidiConnectionState

private val frenchNoteNames =
    listOf("Do", "Do♯", "Ré", "Ré♯", "Mi", "Fa", "Fa♯", "Sol", "Sol♯", "La", "La♯", "Si")

/**
 * French name of a MIDI note ("Do", "Fa♯"…).
 *
 * @param note MIDI note number.
 * @return The solfège name, octave omitted (kid-friendly).
 */
private fun frenchNoteName(note: Int): String {
    return frenchNoteNames[note % 12]
}

/**
 * Discovery mode: the virtual keyboard mirrors what is played on the physical
 * keyboard, golden sparkles rise from the keys, and the names of the played
 * notes are shown big in the center (first note-reading exposure).
 *
 * @param connectionState Keyboard connection status (header icon).
 * @param pressedNotes MIDI note numbers currently held down.
 * @param onBack Called to leave the screen.
 * @param onDebugTouch Debug-build note injection via the virtual keyboard.
 */
@Composable
fun DiscoveryScreen(
    connectionState: MidiConnectionState,
    pressedNotes: Set<Int>,
    onBack: () -> Unit,
    onDebugTouch: ((note: Int, isNoteOn: Boolean) -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = stringResource(R.string.lesson_back),
                )
            }
            Text(
                text = stringResource(R.string.home_discovery_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Lucide.Usb,
                contentDescription = stringResource(
                    if (connectionState is MidiConnectionState.Connected) {
                        R.string.home_connected_description
                    } else {
                        R.string.home_disconnected_description
                    }
                ),
                tint = if (connectionState is MidiConnectionState.Connected) {
                    PianoPalette.correct
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                },
                modifier = Modifier.padding(end = 8.dp).size(22.dp),
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(0.55f),
        ) {
            if (pressedNotes.isEmpty()) {
                Text(
                    text = stringResource(R.string.discovery_hint),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Text(
                    text = pressedNotes.sorted().joinToString(" · ") { note -> frenchNoteName(note) },
                    style = MaterialTheme.typography.headlineLarge,
                    color = PianoPalette.expectedHalo,
                    modifier = Modifier.align(Alignment.Center),
                )
            }
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
