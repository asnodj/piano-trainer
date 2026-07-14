package dev.asnodj.pianotrainer.lesson

import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongNote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/** Result of one note in a tempo run. */
enum class TempoJudgment {
    PENDING,
    PERFECT,
    GOOD,
    MISSED,
}

/**
 * Immutable snapshot of a tempo run, consumed by the UI.
 *
 * @property positionMs Current song position of the run.
 * @property judgments One judgment per note, aligned with [TempoEngine.notes].
 * @property perfectCount Notes hit within the perfect window.
 * @property goodCount Notes hit within the good window.
 * @property missedCount Notes never hit in time.
 * @property wrongPressCount Presses that matched no expected note.
 * @property scorePercent Points earned over the maximum, 0..100.
 * @property finished True when the run reached the end of the song.
 */
data class TempoState(
    val positionMs: Int,
    val judgments: List<TempoJudgment>,
    val perfectCount: Int,
    val goodCount: Int,
    val missedCount: Int,
    val wrongPressCount: Int,
    val scorePercent: Int,
    val finished: Boolean,
)

/**
 * Tempo-mode game engine (the graduation after wait mode): the song scrolls on
 * its own and every note must be hit inside a timing window. Nothing ever
 * pauses; missed notes are just missed. Pure Kotlin, unit-testable — the clock
 * lives outside and feeds [advanceTo].
 *
 * @param song The loaded song.
 * @param handMode Which hand(s) are played; other notes are dropped.
 */
class TempoEngine(song: Song, handMode: HandMode) {

    companion object {
        /** Hit within ±this window of the note start = perfect. */
        const val PERFECT_WINDOW_MS = 120

        /** Hit within ±this window = good; beyond = wrong press / missed. */
        const val GOOD_WINDOW_MS = 300

        private const val PERFECT_POINTS = 100
        private const val GOOD_POINTS = 60
    }

    /** The judged notes of the run, sorted by start time. */
    val notes: List<SongNote> = when (handMode) {
        HandMode.BOTH -> song.notes
        HandMode.RIGHT -> song.notes.filter { songNote -> songNote.hand == Hand.RIGHT }
        HandMode.LEFT -> song.notes.filter { songNote -> songNote.hand == Hand.LEFT }
    }

    private val endMs: Int = song.durationMs
    private val judgments = MutableList(notes.size) { TempoJudgment.PENDING }
    private var wrongPresses = 0

    private val mutableState = MutableStateFlow(snapshot(positionMs = 0))

    /** Current run snapshot for the UI. */
    val state: StateFlow<TempoState> = mutableState.asStateFlow()

    /**
     * Moves the run clock forward: notes whose window has fully passed become
     * missed, and the run finishes past the end of the song.
     *
     * @param positionMs New song position (monotonically increasing).
     */
    fun advanceTo(positionMs: Int) {
        notes.forEachIndexed { noteIndex, songNote ->
            if (judgments[noteIndex] == TempoJudgment.PENDING &&
                songNote.startMs + GOOD_WINDOW_MS < positionMs
            ) {
                judgments[noteIndex] = TempoJudgment.MISSED
            }
        }
        mutableState.value = snapshot(positionMs)
    }

    /**
     * Handles a key press: matches the closest pending note of that pitch
     * within the good window, otherwise counts a wrong press.
     *
     * @param note MIDI note number pressed.
     */
    fun onNoteOn(note: Int) {
        val positionMs = mutableState.value.positionMs
        var bestIndex = -1
        var bestDelta = Int.MAX_VALUE
        notes.forEachIndexed { noteIndex, songNote ->
            if (judgments[noteIndex] == TempoJudgment.PENDING && songNote.midiNote == note) {
                val delta = abs(songNote.startMs - positionMs)
                if (delta <= GOOD_WINDOW_MS && delta < bestDelta) {
                    bestIndex = noteIndex
                    bestDelta = delta
                }
            }
        }
        if (bestIndex >= 0) {
            judgments[bestIndex] =
                if (bestDelta <= PERFECT_WINDOW_MS) TempoJudgment.PERFECT else TempoJudgment.GOOD
        } else {
            wrongPresses++
        }
        mutableState.value = snapshot(positionMs)
    }

    /**
     * Builds the immutable snapshot for the current judgment table.
     *
     * @param positionMs Position to stamp on the snapshot.
     * @return The tempo state.
     */
    private fun snapshot(positionMs: Int): TempoState {
        val perfectCount = judgments.count { judgment -> judgment == TempoJudgment.PERFECT }
        val goodCount = judgments.count { judgment -> judgment == TempoJudgment.GOOD }
        val missedCount = judgments.count { judgment -> judgment == TempoJudgment.MISSED }
        val maxPoints = notes.size * PERFECT_POINTS
        val earnedPoints = perfectCount * PERFECT_POINTS + goodCount * GOOD_POINTS
        return TempoState(
            positionMs = positionMs,
            judgments = judgments.toList(),
            perfectCount = perfectCount,
            goodCount = goodCount,
            missedCount = missedCount,
            wrongPressCount = wrongPresses,
            scorePercent = if (maxPoints == 0) 100 else earnedPoints * 100 / maxPoints,
            finished = positionMs >= endMs + GOOD_WINDOW_MS,
        )
    }
}
