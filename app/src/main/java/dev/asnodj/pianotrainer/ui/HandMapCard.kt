package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import dev.asnodj.pianotrainer.R
import dev.asnodj.pianotrainer.song.Hand

/** Relative fingertip heights, thumb (1) to pinky (5). */
private val fingerHeights = listOf(0.45f, 0.82f, 1f, 0.86f, 0.62f)

/**
 * Small reference card showing both hands with the finger numbering
 * (1 = thumb .. 5 = pinky), matching the badges shown on notes and keys.
 *
 * @param modifier Layout modifier (place it in a corner of the highway).
 */
@Composable
fun HandMapCard(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(Color(0xCC1A2126), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HandDiagram(hand = Hand.LEFT, label = stringResource(R.string.hand_map_left))
        HandDiagram(hand = Hand.RIGHT, label = stringResource(R.string.hand_map_right))
    }
}

/**
 * One schematic hand: five finger bars with numbered fingertip badges,
 * mirrored for the left hand, tinted with the hand's note color.
 *
 * @param hand Which hand to draw.
 * @param label Short label under the diagram ("G" / "D").
 */
@Composable
private fun HandDiagram(hand: Hand, label: String) {
    val textMeasurer = rememberTextMeasurer()
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(modifier = Modifier.width(64.dp).height(52.dp)) {
            val fingerCount = fingerHeights.size
            val slotWidth = size.width / fingerCount
            val barWidth = slotWidth * 0.62f
            fingerHeights.forEachIndexed { fingerIndex, relativeHeight ->
                // Right hand reads thumb-to-pinky left-to-right; left hand mirrored.
                val slotIndex = if (hand == Hand.RIGHT) fingerIndex else fingerCount - 1 - fingerIndex
                val centerX = slotIndex * slotWidth + slotWidth / 2f
                val tipY = size.height * (1f - relativeHeight) + barWidth / 2f
                drawRoundRect(
                    color = handColor(hand).copy(alpha = 0.45f),
                    topLeft = Offset(centerX - barWidth / 2f, tipY - barWidth / 2f),
                    size = Size(barWidth, size.height - tipY + barWidth / 2f),
                    cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
                )
                drawFingerBadge(
                    textMeasurer = textMeasurer,
                    finger = fingerIndex + 1,
                    center = Offset(centerX, tipY),
                    radius = barWidth * 0.52f,
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = handColor(hand),
        )
    }
}
