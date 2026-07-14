package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Sparkles
import dev.asnodj.pianotrainer.R
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.song.Song

/**
 * Home screen: connection status, discovery mode entry and the song list.
 *
 * @param connectionState Keyboard connection status.
 * @param songs Bundled songs, easiest first.
 * @param onOpenDiscovery Called when the discovery card is tapped.
 * @param onOpenSong Called with the tapped song.
 */
@Composable
fun HomeScreen(
    connectionState: MidiConnectionState,
    songs: List<Song>,
    onOpenDiscovery: () -> Unit,
    onOpenSong: (Song) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
        ConnectionStatusRow(connectionState)

        HomeCard(
            icon = Lucide.Sparkles,
            iconTint = PianoPalette.expectedHalo,
            title = stringResource(R.string.home_discovery_title),
            subtitle = stringResource(R.string.home_discovery_subtitle),
            onClick = onOpenDiscovery,
        )
        songs.forEach { song ->
            HomeCard(
                icon = Lucide.Music,
                iconTint = PianoPalette.rightHand,
                title = song.title,
                subtitle = stringResource(R.string.home_song_subtitle, song.notes.size, song.sections.size),
                onClick = { onOpenSong(song) },
            )
        }
    }
}

/**
 * Connection status line: a colored dot plus the status text.
 */
@Composable
private fun ConnectionStatusRow(connectionState: MidiConnectionState) {
    val (dotColor, statusText) = when (connectionState) {
        is MidiConnectionState.Connected ->
            PianoPalette.correct to stringResource(R.string.keyboard_connected, connectionState.deviceName)
        is MidiConnectionState.Connecting ->
            PianoPalette.expectedHalo to stringResource(R.string.keyboard_connecting)
        MidiConnectionState.Disconnected ->
            PianoPalette.wrong to stringResource(R.string.keyboard_disconnected)
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.size(10.dp).background(dotColor, CircleShape))
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One tappable entry of the home list, with a tinted leading icon.
 *
 * @param icon Lucide icon of the entry.
 * @param iconTint Icon color.
 * @param title Entry title.
 * @param subtitle One-line description.
 * @param onClick Tap callback.
 */
@Composable
private fun HomeCard(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                imageVector = Lucide.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
