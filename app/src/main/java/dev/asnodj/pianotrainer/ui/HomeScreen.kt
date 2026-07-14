package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Sparkles
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Usb
import dev.asnodj.pianotrainer.R
import dev.asnodj.pianotrainer.midi.MidiConnectionState
import dev.asnodj.pianotrainer.profile.PROFILES
import dev.asnodj.pianotrainer.song.Song

/**
 * Home screen: logo header with profile picker and connection indicator, then
 * the song library as tiles.
 *
 * @param connectionState Keyboard connection status (drives the header icon).
 * @param songs Bundled songs, easiest first.
 * @param selectedProfileId Active family profile id.
 * @param bestScores Best scores of the active profile, keyed by "songId/mode".
 * @param onSelectProfile Called with the tapped profile id.
 * @param onOpenDiscovery Called when the discovery tile is tapped.
 * @param onOpenSong Called with the tapped song.
 */
@Composable
fun HomeScreen(
    connectionState: MidiConnectionState,
    songs: List<Song>,
    selectedProfileId: String,
    bestScores: Map<String, Int>,
    onSelectProfile: (String) -> Unit,
    onOpenDiscovery: () -> Unit,
    onOpenSong: (Song) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 12.dp)) {
        HomeHeader(
            connectionState = connectionState,
            selectedProfileId = selectedProfileId,
            onSelectProfile = onSelectProfile,
        )
        Spacer(modifier = Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 250.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HomeTile(
                    icon = Lucide.Sparkles,
                    iconTint = PianoPalette.expectedHalo,
                    title = stringResource(R.string.home_discovery_title),
                    subtitle = stringResource(R.string.home_discovery_subtitle),
                    waitScore = null,
                    tempoScore = null,
                    onClick = onOpenDiscovery,
                )
            }
            items(songs) { song ->
                HomeTile(
                    icon = Lucide.Music,
                    iconTint = PianoPalette.rightHand,
                    title = song.title,
                    subtitle = stringResource(R.string.home_song_subtitle, song.notes.size, song.sections.size),
                    waitScore = bestScores["${song.id}/wait"],
                    tempoScore = bestScores["${song.id}/tempo"],
                    onClick = { onOpenSong(song) },
                )
            }
        }
    }
}

/**
 * Header: two-tone logo, profile initials and the connection indicator
 * (green when the keyboard is connected, dimmed otherwise).
 */
@Composable
private fun HomeHeader(
    connectionState: MidiConnectionState,
    selectedProfileId: String,
    onSelectProfile: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.logo_piano),
            style = MaterialTheme.typography.headlineMedium,
            color = PianoPalette.ivory,
        )
        Text(
            text = stringResource(R.string.logo_trainer),
            style = MaterialTheme.typography.headlineMedium,
            color = PianoPalette.expectedHalo,
        )
        Spacer(modifier = Modifier.weight(1f))
        PROFILES.forEach { profile ->
            ProfileBadge(
                initial = profile.displayName.first().toString(),
                selected = profile.id == selectedProfileId,
                contentDescription = profile.displayName,
                onClick = { onSelectProfile(profile.id) },
            )
        }
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
            modifier = Modifier.padding(start = 4.dp).size(24.dp),
        )
    }
}

/**
 * One tappable profile initial: amber ring + bright text when selected.
 *
 * @param initial Single letter shown in the circle.
 * @param selected Whether this profile is active.
 * @param contentDescription Profile name for accessibility.
 * @param onClick Selection callback.
 */
@Composable
private fun ProfileBadge(
    initial: String,
    selected: Boolean,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
            .border(
                width = 2.dp,
                color = if (selected) PianoPalette.expectedHalo else Color.Transparent,
                shape = CircleShape,
            )
            .clickable(onClickLabel = contentDescription) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = if (selected) PianoPalette.expectedHalo else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * One library tile: icon, title, subtitle and the active profile's earned
 * stars (wait mode) plus best tempo score when available.
 */
@Composable
private fun HomeTile(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    waitScore: Int?,
    tempoScore: Int?,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.surface, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.weight(1f))
                if (waitScore != null) {
                    StarsRow(earnedStars = starsForWaitScore(waitScore))
                }
            }
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (tempoScore != null) {
                    Icon(
                        imageVector = Lucide.Gauge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = stringResource(R.string.home_tempo_best, tempoScore),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/**
 * Maps a wait-mode accuracy to stars (same thresholds as the finish overlay).
 */
private fun starsForWaitScore(score: Int): Int {
    return when {
        score >= 95 -> 3
        score >= 80 -> 2
        else -> 1
    }
}

/**
 * Three small stars, earned ones in amber.
 */
@Composable
private fun StarsRow(earnedStars: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(3) { starIndex ->
            Icon(
                imageVector = Lucide.Star,
                contentDescription = null,
                tint = if (starIndex < earnedStars) {
                    PianoPalette.expectedHalo
                } else {
                    PianoPalette.ivoryDim.copy(alpha = 0.3f)
                },
                modifier = Modifier.size(16.dp),
            )
        }
    }
}
