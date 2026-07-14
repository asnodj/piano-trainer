package dev.asnodj.pianotrainer.lesson

import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Which hand(s) the player practices in a lesson. */
enum class HandMode {
    RIGHT,
    LEFT,
    BOTH,
}

/**
 * Notes starting (nearly) together that must all be pressed to advance.
 *
 * @property startMs Start time of the group in the song.
 * @property notes The simultaneous notes (a single note or a chord).
 */
data class NoteGroup(
    val startMs: Int,
    val notes: List<SongNote>,
)

/**
 * Immutable snapshot of a wait-mode lesson, consumed by the UI.
 *
 * @property groupIndex Index of the group the song is frozen on.
 * @property totalGroups Total number of groups in the lesson.
 * @property expectedNotes MIDI notes that must be pressed to advance.
 * @property correctlyPressed Expected notes already pressed (latched).
 * @property wrongHeld Wrong keys currently held down, for red feedback.
 * @property positionMs Song position the display is frozen at.
 * @property finished True when the whole song has been played.
 * @property wrongPressCount Total wrong key presses since the lesson started.
 * @property accuracyPercent Correct notes over total presses, 0..100.
 */
data class LessonState(
    val groupIndex: Int,
    val totalGroups: Int,
    val expectedNotes: Set<Int>,
    val correctlyPressed: Set<Int>,
    val wrongHeld: Set<Int>,
    val positionMs: Int,
    val finished: Boolean,
    val wrongPressCount: Int,
    val accuracyPercent: Int,
)

/**
 * Wait-mode game engine (the DEFAULT learning state of the app): the song is
 * frozen on the current note group and only advances when every expected key
 * has been pressed. Wrong keys are flagged while held but never restart
 * anything. Pure Kotlin, no Android dependency: unit-testable.
 *
 * @param song The loaded song.
 * @param handMode Which hand(s) to practice; other notes are dropped.
 */
class LessonEngine(song: Song, handMode: HandMode) {

    /** Notes starting within this window belong to the same group (chord). */
    private companion object {
        const val CHORD_WINDOW_MS = 50
    }

    private val groups: List<NoteGroup> = buildGroups(filterByHand(song.notes, handMode))
    private val endPositionMs: Int = song.durationMs
    private val totalNoteCount: Int = groups.sumOf { group -> group.notes.size }

    private val mutableState = MutableStateFlow(stateAt(groupIndex = 0, wrongPressCount = 0))

    /** Current lesson snapshot for the UI. */
    val state: StateFlow<LessonState> = mutableState.asStateFlow()

    /** The groups of the lesson, exposed for rendering the note highway. */
    val noteGroups: List<NoteGroup> = groups

    /**
     * Handles a key press from the MIDI keyboard.
     * Correct expected keys latch; when the whole group is latched the song
     * advances to the next group. Anything else is flagged as wrong while held.
     *
     * @param note MIDI note number pressed.
     */
    fun onNoteOn(note: Int) {
        val current = mutableState.value
        if (current.finished) {
            return
        }
        if (note in current.expectedNotes) {
            val latched = current.correctlyPressed + note
            if (latched == current.expectedNotes) {
                mutableState.value = stateAt(current.groupIndex + 1, current.wrongPressCount)
                    .copy(wrongHeld = current.wrongHeld)
            } else {
                mutableState.value = current.copy(correctlyPressed = latched)
            }
        } else {
            mutableState.value = current.copy(
                wrongHeld = current.wrongHeld + note,
                wrongPressCount = current.wrongPressCount + 1,
                accuracyPercent = accuracyOf(current.wrongPressCount + 1),
            )
        }
    }

    /**
     * Jumps to a given group, keeping the accumulated score. Used by the seek
     * bar, the rewind button and the hand-switch position continuity.
     *
     * @param targetGroupIndex Group to freeze on (clamped; groups.size = end).
     */
    fun seekToGroup(targetGroupIndex: Int) {
        val clampedIndex = targetGroupIndex.coerceIn(0, groups.size)
        mutableState.value = stateAt(clampedIndex, mutableState.value.wrongPressCount)
    }

    /**
     * Jumps to a position expressed as a fraction of the song.
     *
     * @param fraction 0.0 = beginning, 1.0 = end.
     */
    fun seekToFraction(fraction: Float) {
        seekToGroup((fraction.coerceIn(0f, 1f) * groups.size).toInt())
    }

    /**
     * Jumps to the first group at or after the given song position. Used to
     * stay in place when the practiced hand changes (the new engine has
     * different groups but the same timeline).
     *
     * @param positionMs Song position to resume from.
     */
    fun seekToMs(positionMs: Int) {
        val groupIndex = groups.indexOfFirst { group -> group.startMs >= positionMs }
        seekToGroup(if (groupIndex >= 0) groupIndex else groups.size)
    }

    /**
     * Handles a key release: clears the red feedback of a wrong key.
     *
     * @param note MIDI note number released.
     */
    fun onNoteOff(note: Int) {
        val current = mutableState.value
        if (note in current.wrongHeld) {
            mutableState.value = current.copy(wrongHeld = current.wrongHeld - note)
        }
    }

    /**
     * Builds the frozen state for a given group index.
     *
     * @param groupIndex Group to freeze on; past the last group means finished.
     * @param wrongPressCount Wrong presses accumulated so far.
     * @return The corresponding lesson state.
     */
    private fun stateAt(groupIndex: Int, wrongPressCount: Int): LessonState {
        val finished = groupIndex >= groups.size
        return LessonState(
            groupIndex = groupIndex,
            totalGroups = groups.size,
            expectedNotes = if (finished) emptySet() else groups[groupIndex].notes.map { songNote -> songNote.midiNote }.toSet(),
            correctlyPressed = emptySet(),
            wrongHeld = emptySet(),
            positionMs = if (finished) endPositionMs else groups[groupIndex].startMs,
            finished = finished,
            wrongPressCount = wrongPressCount,
            accuracyPercent = accuracyOf(wrongPressCount),
        )
    }

    /**
     * Computes the accuracy: notes of the song over total presses needed.
     *
     * @param wrongPressCount Wrong presses so far.
     * @return Percentage 0..100 (100 = no wrong press at all).
     */
    private fun accuracyOf(wrongPressCount: Int): Int {
        if (totalNoteCount == 0) {
            return 100
        }
        return (totalNoteCount * 100) / (totalNoteCount + wrongPressCount)
    }

    /**
     * Keeps only the notes of the practiced hand(s).
     *
     * @param notes All song notes.
     * @param handMode Practice mode.
     * @return The filtered notes, still sorted.
     */
    private fun filterByHand(notes: List<SongNote>, handMode: HandMode): List<SongNote> {
        return when (handMode) {
            HandMode.BOTH -> notes
            HandMode.RIGHT -> notes.filter { songNote -> songNote.hand == Hand.RIGHT }
            HandMode.LEFT -> notes.filter { songNote -> songNote.hand == Hand.LEFT }
        }
    }

    /**
     * Groups notes whose starts fall within [CHORD_WINDOW_MS] of the group start.
     *
     * @param notes Sorted song notes.
     * @return The chronological note groups.
     */
    private fun buildGroups(notes: List<SongNote>): List<NoteGroup> {
        val builtGroups = mutableListOf<NoteGroup>()
        var currentGroup = mutableListOf<SongNote>()
        notes.forEach { songNote ->
            if (currentGroup.isEmpty() || songNote.startMs - currentGroup.first().startMs <= CHORD_WINDOW_MS) {
                currentGroup.add(songNote)
            } else {
                builtGroups.add(NoteGroup(currentGroup.first().startMs, currentGroup.toList()))
                currentGroup = mutableListOf(songNote)
            }
        }
        if (currentGroup.isNotEmpty()) {
            builtGroups.add(NoteGroup(currentGroup.first().startMs, currentGroup.toList()))
        }
        return builtGroups
    }
}
