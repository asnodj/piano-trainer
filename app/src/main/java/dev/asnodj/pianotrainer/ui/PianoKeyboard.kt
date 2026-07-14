package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import dev.asnodj.pianotrainer.song.Hand

/** Lowest key of the Yamaha PSR-E383 (C2). */
const val KEYBOARD_FIRST_NOTE = 36

/** Highest key of the Yamaha PSR-E383 (C7). */
const val KEYBOARD_LAST_NOTE = 96

/** Number of white keys in the PSR-E383 range. */
const val WHITE_KEY_COUNT = 36

private val whiteKeyColor = Color.White
private val whiteKeyBorderColor = Color(0xFF9E9E9E)
private val blackKeyColor = Color(0xFF212121)

/**
 * Wait-mode hint for one key: which hand should play it and with which finger.
 *
 * @property hand Hand that plays the key (drives the hint color).
 * @property finger Finger number 1..5, or null when not authored.
 */
data class ExpectedKey(
    val hand: Hand,
    val finger: Int?,
)

/**
 * Tells whether a MIDI note is a black key.
 *
 * @param note MIDI note number.
 * @return True for C#, D#, F#, G#, A#.
 */
fun isBlackKey(note: Int): Boolean {
    return when (note % 12) {
        1, 3, 6, 8, 10 -> true
        else -> false
    }
}

/**
 * Horizontal position of a key, as a fraction of the keyboard width, shared by
 * the keyboard and the falling-notes highway so notes land on their key.
 *
 * @param note MIDI note number within the keyboard range.
 * @return Left edge x in 0..1.
 */
fun keyLeftFraction(note: Int): Float {
    val whiteKeysBefore = (KEYBOARD_FIRST_NOTE until note).count { lowerNote -> !isBlackKey(lowerNote) }
    val whiteKeyFraction = 1f / WHITE_KEY_COUNT
    return if (isBlackKey(note)) {
        whiteKeysBefore * whiteKeyFraction - keyWidthFraction(note) / 2f
    } else {
        whiteKeysBefore * whiteKeyFraction
    }
}

/**
 * Width of a key as a fraction of the keyboard width.
 *
 * @param note MIDI note number.
 * @return Width in 0..1.
 */
fun keyWidthFraction(note: Int): Float {
    val whiteKeyFraction = 1f / WHITE_KEY_COUNT
    return if (isBlackKey(note)) whiteKeyFraction * 0.6f else whiteKeyFraction
}

/**
 * Resolves the hand color used across the keyboard and the note highway.
 *
 * @param hand Hand playing the note.
 * @return Sky blue for right, violet for left.
 */
fun handColor(hand: Hand): Color {
    return if (hand == Hand.LEFT) PianoPalette.leftHand else PianoPalette.rightHand
}

/**
 * 61-key piano keyboard (PSR-E383 range) drawn on a Canvas. Display only —
 * touch input is out of scope (MIDI-first by design).
 *
 * Key states, by priority: wrong (red) > pressed (green) > expected (hand
 * color + amber halo + finger badge).
 *
 * @param pressedNotes Keys currently held down / validated, shown green.
 * @param modifier Layout modifier; the keyboard fills the space it is given.
 * @param expectedKeys Keys to play next with their hand/finger hint.
 * @param wrongNotes Wrong keys currently held, shown red.
 */
@Composable
fun PianoKeyboard(
    pressedNotes: Set<Int>,
    modifier: Modifier = Modifier,
    expectedKeys: Map<Int, ExpectedKey> = emptyMap(),
    wrongNotes: Set<Int> = emptySet(),
) {
    val allNotes = KEYBOARD_FIRST_NOTE..KEYBOARD_LAST_NOTE
    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = modifier) {
        val blackKeyHeight = size.height * 0.62f

        allNotes.filterNot(::isBlackKey).forEach { note ->
            drawKey(note, size.height, pressedNotes, expectedKeys, wrongNotes, textMeasurer)
        }
        allNotes.filter(::isBlackKey).forEach { note ->
            drawKey(note, blackKeyHeight, pressedNotes, expectedKeys, wrongNotes, textMeasurer)
        }
    }
}

/**
 * Draws one key with its state overlay and optional finger badge.
 */
private fun DrawScope.drawKey(
    note: Int,
    keyHeight: Float,
    pressedNotes: Set<Int>,
    expectedKeys: Map<Int, ExpectedKey>,
    wrongNotes: Set<Int>,
    textMeasurer: TextMeasurer,
) {
    val isBlack = isBlackKey(note)
    val keyTopLeft = Offset(keyLeftFraction(note) * size.width, 0f)
    val keySize = Size(keyWidthFraction(note) * size.width, keyHeight)
    val expected = expectedKeys[note]

    val fillColor = when {
        note in wrongNotes -> PianoPalette.wrong
        note in pressedNotes -> PianoPalette.correct
        else -> if (isBlack) blackKeyColor else whiteKeyColor
    }
    drawRect(color = fillColor, topLeft = keyTopLeft, size = keySize)
    if (!isBlack) {
        drawRect(color = whiteKeyBorderColor, topLeft = keyTopLeft, size = keySize, style = Stroke(width = 1f))
    }

    if (expected != null && note !in pressedNotes && note !in wrongNotes) {
        // Hand-colored hint with the amber "play me" halo.
        drawRect(color = handColor(expected.hand).copy(alpha = 0.55f), topLeft = keyTopLeft, size = keySize)
        drawRect(color = PianoPalette.expectedHalo, topLeft = keyTopLeft, size = keySize, style = Stroke(width = 4f))
        if (expected.finger != null) {
            drawFingerBadge(
                textMeasurer = textMeasurer,
                finger = expected.finger,
                center = Offset(keyTopLeft.x + keySize.width / 2f, keyHeight * 0.78f),
                radius = keySize.width * 0.34f,
            )
        }
    }
}

/**
 * Draws the signature fingering badge: an ivory disc with the finger number,
 * used on both the keyboard keys and the falling notes.
 *
 * @param textMeasurer Shared measurer for the digit.
 * @param finger Finger number 1..5.
 * @param center Badge center.
 * @param radius Badge radius in px.
 */
fun DrawScope.drawFingerBadge(
    textMeasurer: TextMeasurer,
    finger: Int,
    center: Offset,
    radius: Float,
) {
    drawCircle(color = PianoPalette.ivory, radius = radius, center = center)
    drawCircle(color = Color(0x33000000), radius = radius, center = center, style = Stroke(width = 2f))
    val measured = textMeasurer.measure(
        text = finger.toString(),
        style = TextStyle(
            color = Color(0xFF1A2126),
            fontSize = (radius * 1.2f).toSp(),
            fontWeight = FontWeight.Bold,
        ),
    )
    drawText(
        textLayoutResult = measured,
        topLeft = Offset(
            center.x - measured.size.width / 2f,
            center.y - measured.size.height / 2f,
        ),
    )
}
