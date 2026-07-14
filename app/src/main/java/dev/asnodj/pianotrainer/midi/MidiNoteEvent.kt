package dev.asnodj.pianotrainer.midi

/**
 * A single note-on or note-off event received from the MIDI keyboard.
 *
 * @property note MIDI note number (middle C = 60; PSR-E383 range is 36..96).
 * @property velocity Key velocity 1..127 for note-on, 0 for note-off.
 * @property isNoteOn True for a key press, false for a key release.
 * @property channel MIDI channel 0..15 the event was received on.
 * @property timestampNanos Event timestamp in the System.nanoTime() time base.
 */
data class MidiNoteEvent(
    val note: Int,
    val velocity: Int,
    val isNoteOn: Boolean,
    val channel: Int,
    val timestampNanos: Long,
)
