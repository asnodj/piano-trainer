package dev.asnodj.pianotrainer.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke

/** Lowest key of the Yamaha PSR-E383 (C2). */
const val KEYBOARD_FIRST_NOTE = 36

/** Highest key of the Yamaha PSR-E383 (C7). */
const val KEYBOARD_LAST_NOTE = 96

private val whiteKeyColor = Color.White
private val whiteKeyBorderColor = Color(0xFF9E9E9E)
private val blackKeyColor = Color(0xFF212121)
private val pressedKeyColor = Color(0xFF4CAF50)

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
 * 61-key piano keyboard (PSR-E383 range) drawn on a Canvas, with pressed keys
 * highlighted in green. Display only for now — touch input is out of scope.
 *
 * @param pressedNotes MIDI note numbers currently held down.
 * @param modifier Layout modifier; the keyboard fills the space it is given.
 */
@Composable
fun PianoKeyboard(pressedNotes: Set<Int>, modifier: Modifier = Modifier) {
    val allNotes = KEYBOARD_FIRST_NOTE..KEYBOARD_LAST_NOTE
    val whiteNotes = allNotes.filter { note -> !isBlackKey(note) }

    Canvas(modifier = modifier) {
        val whiteKeyWidth = size.width / whiteNotes.size
        val blackKeyWidth = whiteKeyWidth * 0.6f
        val blackKeyHeight = size.height * 0.62f

        whiteNotes.forEachIndexed { whiteIndex, note ->
            val keyTopLeft = Offset(whiteIndex * whiteKeyWidth, 0f)
            val keySize = Size(whiteKeyWidth, size.height)
            drawRect(
                color = if (note in pressedNotes) pressedKeyColor else whiteKeyColor,
                topLeft = keyTopLeft,
                size = keySize,
            )
            drawRect(
                color = whiteKeyBorderColor,
                topLeft = keyTopLeft,
                size = keySize,
                style = Stroke(width = 1f),
            )
        }

        allNotes.filter(::isBlackKey).forEach { note ->
            // A black key sits astride the boundary before the next white key.
            val whiteKeysBefore = (KEYBOARD_FIRST_NOTE until note).count { lowerNote ->
                !isBlackKey(lowerNote)
            }
            drawRect(
                color = if (note in pressedNotes) pressedKeyColor else blackKeyColor,
                topLeft = Offset(whiteKeysBefore * whiteKeyWidth - blackKeyWidth / 2f, 0f),
                size = Size(blackKeyWidth, blackKeyHeight),
            )
        }
    }
}
