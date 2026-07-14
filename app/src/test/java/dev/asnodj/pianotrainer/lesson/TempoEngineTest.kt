package dev.asnodj.pianotrainer.lesson

import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TempoEngineTest {

    private fun note(midiNote: Int, startMs: Int, hand: Hand = Hand.RIGHT): SongNote {
        return SongNote(midiNote = midiNote, startMs = startMs, durationMs = 400, hand = hand, finger = null)
    }

    private fun songOf(vararg notes: SongNote): Song {
        return Song(
            id = "test",
            title = "Test song",
            notes = notes.toList(),
            sections = emptyList(),
            durationMs = notes.maxOf { songNote -> songNote.startMs + songNote.durationMs },
        )
    }

    @Test
    fun `hit inside the perfect window scores perfect`() {
        // Prepare
        val engine = TempoEngine(songOf(note(60, 1000)), HandMode.BOTH)

        // Act
        engine.advanceTo(1050)
        engine.onNoteOn(60)

        // Assert
        assertEquals(1, engine.state.value.perfectCount)
        assertEquals(listOf(TempoJudgment.PERFECT), engine.state.value.judgments)
    }

    @Test
    fun `hit inside the good window but outside perfect scores good`() {
        // Prepare
        val engine = TempoEngine(songOf(note(60, 1000)), HandMode.BOTH)

        // Act
        engine.advanceTo(1250)
        engine.onNoteOn(60)

        // Assert
        assertEquals(1, engine.state.value.goodCount)
        assertEquals(0, engine.state.value.perfectCount)
    }

    @Test
    fun `note not hit before its window closes becomes missed`() {
        // Prepare
        val engine = TempoEngine(songOf(note(60, 1000), note(62, 2000)), HandMode.BOTH)

        // Act
        engine.advanceTo(1400)

        // Assert
        assertEquals(1, engine.state.value.missedCount)
        assertEquals(TempoJudgment.MISSED, engine.state.value.judgments[0])
        assertEquals(TempoJudgment.PENDING, engine.state.value.judgments[1])
    }

    @Test
    fun `press with no matching note counts as wrong press`() {
        // Prepare
        val engine = TempoEngine(songOf(note(60, 1000)), HandMode.BOTH)

        // Act: right pitch but way too early.
        engine.advanceTo(100)
        engine.onNoteOn(60)
        // Wrong pitch inside the window.
        engine.advanceTo(1000)
        engine.onNoteOn(65)

        // Assert
        assertEquals(2, engine.state.value.wrongPressCount)
        assertEquals(0, engine.state.value.perfectCount)
    }

    @Test
    fun `repeated same-pitch notes are matched to the closest pending one`() {
        // Prepare: two C4 close together, both windows still open at 1450ms.
        val engine = TempoEngine(songOf(note(60, 1200), note(60, 1500)), HandMode.BOTH)

        // Act
        engine.advanceTo(1450)
        engine.onNoteOn(60)

        // Assert: the 1500ms note is matched (delta 50), not the 1200ms one (delta 250).
        assertEquals(TempoJudgment.PENDING, engine.state.value.judgments[0])
        assertEquals(TempoJudgment.PERFECT, engine.state.value.judgments[1])
    }

    @Test
    fun `score combines perfect and good points`() {
        // Prepare: two notes, one perfect (100) + one good (60) = 160/200 = 80%.
        val engine = TempoEngine(songOf(note(60, 1000), note(62, 2000)), HandMode.BOTH)

        // Act
        engine.advanceTo(1000)
        engine.onNoteOn(60)
        engine.advanceTo(2200)
        engine.onNoteOn(62)

        // Assert
        assertEquals(80, engine.state.value.scorePercent)
    }

    @Test
    fun `run finishes after the end of the song`() {
        // Prepare
        val engine = TempoEngine(songOf(note(60, 1000)), HandMode.BOTH)

        // Act
        engine.advanceTo(1200)
        assertFalse(engine.state.value.finished)
        engine.advanceTo(1400 + 300)

        // Assert: finished, and the never-pressed note ended up missed.
        assertTrue(engine.state.value.finished)
        assertEquals(1, engine.state.value.missedCount)
    }

    @Test
    fun `hand filter drops the other hand from judged notes`() {
        // Prepare
        val engine = TempoEngine(
            songOf(note(48, 0, Hand.LEFT), note(60, 0), note(62, 600)),
            HandMode.RIGHT,
        )

        // Assert
        assertEquals(2, engine.notes.size)
        assertTrue(engine.notes.all { songNote -> songNote.hand == Hand.RIGHT })
    }
}
