package dev.asnodj.pianotrainer.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Gauge
import com.composables.icons.lucide.Hand
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.RotateCcw
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Star
import dev.asnodj.pianotrainer.R
import dev.asnodj.pianotrainer.SPEED_OPTIONS
import dev.asnodj.pianotrainer.lesson.HandMode
import dev.asnodj.pianotrainer.lesson.LessonState
import dev.asnodj.pianotrainer.lesson.NoteGroup
import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.SongNote

/** How far ahead (in song ms) the note highway shows upcoming notes. */
private const val LOOKAHEAD_MS = 4000f

/**
 * Lesson screen: falling-notes highway above the virtual keyboard. Frozen in
 * wait mode by default; scrolls with the audio during demo playback.
 *
 * @param songTitle Title shown in the header.
 * @param lessonState Current engine snapshot.
 * @param physicalPressedNotes Keys physically held on the MIDI keyboard,
 *   drives the key-press sparkle bursts.
 * @param noteGroups All groups of the lesson, for rendering upcoming notes.
 * @param handMode Currently practiced hand(s).
 * @param leftHandAvailable True when the song has left-hand notes.
 * @param rightHandAvailable True when the song has right-hand notes.
 * @param speedFactor Tempo multiplier for playback and scrolling.
 * @param isPlaying True while the demo playback runs.
 * @param playbackPositionMs Playback position driving the highway when playing.
 * @param onToggleHand Called when a hand switch is tapped.
 * @param onSpeedChange Called with the picked tempo multiplier.
 * @param onTogglePlayback Called when the play/stop button is tapped.
 * @param onSeek Called with a 0..1 fraction when the player scrubs the song.
 * @param onRestart Called when the player restarts the song.
 * @param onBack Called to leave the lesson.
 * @param onDebugTouch Debug-build note injection via the virtual keyboard.
 */
@Composable
fun LessonScreen(
    songTitle: String,
    lessonState: LessonState,
    physicalPressedNotes: Set<Int>,
    noteGroups: List<NoteGroup>,
    handMode: HandMode,
    leftHandAvailable: Boolean,
    rightHandAvailable: Boolean,
    speedFactor: Float,
    isPlaying: Boolean,
    playbackPositionMs: Int,
    onToggleHand: (Hand) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    onRestart: () -> Unit,
    onBack: () -> Unit,
    onDebugTouch: ((note: Int, isNoteOn: Boolean) -> Unit)? = null,
) {
    val expectedKeys = if (isPlaying) emptyMap() else expectedKeysOf(lessonState, noteGroups)

    Column(modifier = Modifier.fillMaxSize()) {
        LessonHeader(
            songTitle = songTitle,
            lessonState = lessonState,
            handMode = handMode,
            leftHandAvailable = leftHandAvailable,
            rightHandAvailable = rightHandAvailable,
            speedFactor = speedFactor,
            isPlaying = isPlaying,
            onToggleHand = onToggleHand,
            onSpeedChange = onSpeedChange,
            onTogglePlayback = onTogglePlayback,
            onSeek = onSeek,
            onBack = onBack,
        )
        Slider(
            value = if (lessonState.totalGroups == 0) {
                0f
            } else {
                lessonState.groupIndex.toFloat() / lessonState.totalGroups
            },
            onValueChange = onSeek,
            modifier = Modifier.fillMaxWidth().height(20.dp),
        )
        Box(modifier = Modifier.fillMaxWidth().weight(0.62f)) {
            NoteHighway(
                lessonState = lessonState,
                noteGroups = noteGroups,
                speedFactor = speedFactor,
                isPlaying = isPlaying,
                playbackPositionMs = playbackPositionMs,
                modifier = Modifier.fillMaxSize(),
            )
            KeySparkles(
                pressedNotes = physicalPressedNotes,
                colorFor = { note ->
                    when {
                        note in lessonState.wrongHeld -> PianoPalette.wrong
                        else -> expectedKeysOf(lessonState, noteGroups)[note]
                            ?.let { expectedKey -> handColor(expectedKey.hand) }
                            ?: PianoPalette.correct
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            HandMapCard(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp),
            )
            if (lessonState.finished && !isPlaying) {
                FinishedOverlay(
                    accuracyPercent = lessonState.accuracyPercent,
                    onRestart = onRestart,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        PianoKeyboard(
            pressedNotes = lessonState.correctlyPressed,
            expectedKeys = expectedKeys,
            wrongNotes = lessonState.wrongHeld,
            onDebugTouch = onDebugTouch,
            modifier = Modifier.fillMaxWidth().weight(0.38f),
        )
    }
}

/**
 * Builds the wait-mode hints of the current group (hand + finger per key).
 *
 * @param lessonState Current engine snapshot.
 * @param noteGroups All lesson groups.
 * @return Expected keys of the frozen group, empty when finished.
 */
private fun expectedKeysOf(lessonState: LessonState, noteGroups: List<NoteGroup>): Map<Int, ExpectedKey> {
    val currentGroup = noteGroups.getOrNull(lessonState.groupIndex) ?: return emptyMap()
    return currentGroup.notes.associate { songNote ->
        songNote.midiNote to ExpectedKey(hand = songNote.hand, finger = songNote.finger)
    }
}

/**
 * Header row: back, title + progress, play/stop, tempo menu and one switch per
 * hand (both on = both hands; the last one cannot be turned off).
 */
@Composable
private fun LessonHeader(
    songTitle: String,
    lessonState: LessonState,
    handMode: HandMode,
    leftHandAvailable: Boolean,
    rightHandAvailable: Boolean,
    speedFactor: Float,
    isPlaying: Boolean,
    onToggleHand: (Hand) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onTogglePlayback: () -> Unit,
    onSeek: (Float) -> Unit,
    onBack: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Lucide.ArrowLeft,
                contentDescription = stringResource(R.string.lesson_back),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = songTitle, style = MaterialTheme.typography.titleMedium)
            Text(
                text = stringResource(R.string.lesson_progress, lessonState.groupIndex, lessonState.totalGroups),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = { onSeek(0f) }) {
            Icon(
                imageVector = Lucide.RotateCcw,
                contentDescription = stringResource(R.string.lesson_rewind),
            )
        }
        IconButton(onClick = onTogglePlayback) {
            Icon(
                imageVector = if (isPlaying) Lucide.Square else Lucide.Play,
                contentDescription = stringResource(
                    if (isPlaying) R.string.lesson_stop else R.string.lesson_play
                ),
                tint = PianoPalette.expectedHalo,
            )
        }
        SpeedMenu(speedFactor = speedFactor, onSpeedChange = onSpeedChange)
        HandSwitch(
            hand = Hand.LEFT,
            active = handMode != HandMode.RIGHT,
            enabled = leftHandAvailable,
            onClick = { onToggleHand(Hand.LEFT) },
        )
        HandSwitch(
            hand = Hand.RIGHT,
            active = handMode != HandMode.LEFT,
            enabled = rightHandAvailable,
            onClick = { onToggleHand(Hand.RIGHT) },
        )
    }
}

/**
 * Tempo selector: a compact "1×" button opening the speed menu.
 */
@Composable
private fun SpeedMenu(speedFactor: Float, onSpeedChange: (Float) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { menuOpen = true }) {
            Icon(
                imageVector = Lucide.Gauge,
                contentDescription = stringResource(R.string.lesson_speed),
                modifier = Modifier.size(18.dp),
            )
            Text(text = formatSpeed(speedFactor), modifier = Modifier.padding(start = 6.dp))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            SPEED_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = formatSpeed(option)) },
                    onClick = {
                        menuOpen = false
                        onSpeedChange(option)
                    },
                )
            }
        }
    }
}

/**
 * Formats a tempo multiplier ("0.75" -> "0,75×", "1.0" -> "1×").
 */
private fun formatSpeed(factor: Float): String {
    val text = if (factor % 1f == 0f) factor.toInt().toString() else factor.toString().replace('.', ',')
    return "$text×"
}

/**
 * Icon-only hand switch, tinted with the hand's note color when active and
 * greyed out when the song has no notes for that hand.
 *
 * @param hand Hand this switch controls.
 * @param active Whether the hand is currently practiced.
 * @param enabled False when the song has no notes for this hand.
 * @param onClick Toggle callback.
 */
@Composable
private fun HandSwitch(hand: Hand, active: Boolean, enabled: Boolean, onClick: () -> Unit) {
    FilterChip(
        selected = active && enabled,
        onClick = onClick,
        enabled = enabled,
        label = {},
        leadingIcon = {
            Icon(
                imageVector = Lucide.Hand,
                contentDescription = stringResource(
                    if (hand == Hand.LEFT) R.string.lesson_hand_left else R.string.lesson_hand_right
                ),
                tint = when {
                    !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                    active -> handColor(hand)
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer(scaleX = if (hand == Hand.LEFT) -1f else 1f),
            )
        },
    )
}

/**
 * Celebration overlay shown when the song is completed: stars (3 from 95%
 * accuracy, 2 from 80%, 1 below — never zero, soft-gate philosophy), the
 * accuracy score and the restart button.
 *
 * @param accuracyPercent Final accuracy 0..100.
 * @param onRestart Restart callback.
 * @param modifier Layout modifier.
 */
@Composable
private fun FinishedOverlay(
    accuracyPercent: Int,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val earnedStars = when {
        accuracyPercent >= 95 -> 3
        accuracyPercent >= 80 -> 2
        else -> 1
    }
    Box(modifier = modifier.background(Color(0xB0000000))) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(R.string.lesson_finished),
                style = MaterialTheme.typography.headlineLarge,
                color = PianoPalette.ivory,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { starIndex ->
                    Icon(
                        imageVector = Lucide.Star,
                        contentDescription = null,
                        tint = if (starIndex < earnedStars) {
                            PianoPalette.expectedHalo
                        } else {
                            PianoPalette.ivoryDim.copy(alpha = 0.35f)
                        },
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
            Text(
                text = stringResource(R.string.lesson_score, accuracyPercent),
                style = MaterialTheme.typography.titleMedium,
                color = PianoPalette.ivory,
            )
            Button(onClick = onRestart) {
                Icon(
                    imageVector = Lucide.RotateCcw,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = stringResource(R.string.lesson_restart),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

/**
 * The falling-notes highway. In wait mode the position is frozen at the
 * current group and advances at song tempo (scaled by the speed factor) when
 * the player hits the right key, so long notes keep sliding below the line for
 * their whole duration. During demo playback the highway follows the audio.
 *
 * @param lessonState Current engine snapshot.
 * @param noteGroups All lesson groups.
 * @param speedFactor Tempo multiplier.
 * @param isPlaying True while the demo playback runs.
 * @param playbackPositionMs Audio position when playing.
 * @param modifier Layout modifier.
 */
@Composable
private fun NoteHighway(
    lessonState: LessonState,
    noteGroups: List<NoteGroup>,
    speedFactor: Float,
    isPlaying: Boolean,
    playbackPositionMs: Int,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val targetMs = if (isPlaying) playbackPositionMs else lessonState.positionMs
    val animatedPosition = remember { Animatable(targetMs.toFloat()) }
    LaunchedEffect(targetMs, speedFactor, isPlaying) {
        val target = targetMs.toFloat()
        when {
            // Playback publishes a continuous ~60 Hz position: follow it directly.
            isPlaying -> animatedPosition.snapTo(target)
            target > animatedPosition.value -> {
                // Wait-mode advance: scroll at song tempo (scaled by the speed
                // factor), capped so very long notes never stall for seconds.
                val travelMs = ((target - animatedPosition.value) / speedFactor).toInt().coerceIn(120, 2000)
                animatedPosition.animateTo(target, tween(durationMillis = travelMs, easing = LinearEasing))
            }
            // Restart, hand-mode change or playback stop: jump back silently.
            else -> animatedPosition.snapTo(target)
        }
    }

    Canvas(modifier = modifier.clipToBounds()) {
        drawRect(color = PianoPalette.highway, size = size)

        noteGroups.forEachIndexed { groupIndex, group ->
            group.notes.forEach { songNote ->
                val relativeStartMs = songNote.startMs - animatedPosition.value
                if (relativeStartMs <= LOOKAHEAD_MS) {
                    drawFallingNote(
                        textMeasurer = textMeasurer,
                        songNote = songNote,
                        relativeStartMs = relativeStartMs,
                        isExpected = !isPlaying && groupIndex == lessonState.groupIndex,
                    )
                }
            }
        }
    }
}

/**
 * Draws one falling note colored by hand, with the amber halo and fingering
 * badge when it is the note to play.
 *
 * @param textMeasurer Shared measurer for badge digits.
 * @param songNote The note to draw.
 * @param relativeStartMs Note start relative to the current scroll position.
 * @param isExpected True when the note belongs to the frozen group.
 */
private fun DrawScope.drawFallingNote(
    textMeasurer: TextMeasurer,
    songNote: SongNote,
    relativeStartMs: Float,
    isExpected: Boolean,
) {
    val noteLeft = keyLeftFraction(songNote.midiNote) * size.width
    val noteWidth = keyWidthFraction(songNote.midiNote) * size.width
    val noteBottom = size.height - relativeStartMs / LOOKAHEAD_MS * size.height
    val noteHeight = songNote.durationMs / LOOKAHEAD_MS * size.height

    val topLeft = Offset(noteLeft + 1f, noteBottom - noteHeight)
    val noteSize = Size(noteWidth - 2f, noteHeight - 3f)
    if (topLeft.y + noteSize.height < 0f || topLeft.y > size.height) {
        return
    }

    drawRoundRect(
        // The tile to play lights up: brighter fill plus a white-hot outline.
        color = if (isExpected) {
            lerp(handColor(songNote.hand), Color.White, 0.35f)
        } else {
            handColor(songNote.hand)
        },
        topLeft = topLeft,
        size = noteSize,
        cornerRadius = CornerRadius(8f, 8f),
    )
    if (isExpected) {
        drawRoundRect(
            color = PianoPalette.nextNoteGlow,
            topLeft = topLeft,
            size = noteSize,
            cornerRadius = CornerRadius(8f, 8f),
            style = Stroke(width = 5f),
        )
        if (songNote.finger != null) {
            drawFingerBadge(
                textMeasurer = textMeasurer,
                finger = songNote.finger,
                center = Offset(
                    topLeft.x + noteSize.width / 2f,
                    (topLeft.y + noteSize.height - noteSize.width * 0.5f).coerceAtLeast(noteSize.width * 0.5f),
                ),
                radius = noteSize.width * 0.36f,
            )
        }
    }
}
