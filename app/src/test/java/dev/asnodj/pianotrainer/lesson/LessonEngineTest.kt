package dev.asnodj.pianotrainer.lesson

import dev.asnodj.pianotrainer.song.Hand
import dev.asnodj.pianotrainer.song.Song
import dev.asnodj.pianotrainer.song.SongNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonEngineTest {

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
    fun `starts frozen on the first group`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        val state = engine.state.value

        // Assert
        assertEquals(setOf(60), state.expectedNotes)
        assertEquals(0, state.positionMs)
        assertFalse(state.finished)
        assertEquals(2, state.totalGroups)
    }

    @Test
    fun `correct key advances to the next group`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(60)

        // Assert
        val state = engine.state.value
        assertEquals(1, state.groupIndex)
        assertEquals(setOf(62), state.expectedNotes)
        assertEquals(600, state.positionMs)
    }

    @Test
    fun `chord requires every note before advancing`() {
        // Prepare: C-E played together, then G.
        val engine = LessonEngine(songOf(note(60, 0), note(64, 20), note(67, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(60)

        // Assert: still frozen, C latched.
        assertEquals(0, engine.state.value.groupIndex)
        assertEquals(setOf(60), engine.state.value.correctlyPressed)

        // Act: complete the chord.
        engine.onNoteOn(64)

        // Assert
        assertEquals(1, engine.state.value.groupIndex)
        assertEquals(setOf(67), engine.state.value.expectedNotes)
    }

    @Test
    fun `wrong key is flagged while held and never advances the song`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(65)

        // Assert
        val state = engine.state.value
        assertEquals(0, state.groupIndex)
        assertEquals(setOf(65), state.wrongHeld)

        // Act: releasing clears the feedback.
        engine.onNoteOff(65)

        // Assert
        assertTrue(engine.state.value.wrongHeld.isEmpty())
    }

    @Test
    fun `completing the last group finishes the lesson`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(60)
        engine.onNoteOn(62)

        // Assert
        val state = engine.state.value
        assertTrue(state.finished)
        assertTrue(state.expectedNotes.isEmpty())

        // Act: further presses are ignored.
        engine.onNoteOn(60)
        assertTrue(engine.state.value.finished)
    }

    @Test
    fun `right-hand mode drops left-hand notes`() {
        // Prepare
        val engine = LessonEngine(
            songOf(note(48, 0, Hand.LEFT), note(60, 0), note(62, 600)),
            HandMode.RIGHT,
        )

        // Act
        val state = engine.state.value

        // Assert: the left-hand C3 is not part of the lesson.
        assertEquals(setOf(60), state.expectedNotes)
        assertEquals(2, state.totalGroups)
    }

    @Test
    fun `both-hands mode groups simultaneous left and right notes as one chord`() {
        // Prepare
        val engine = LessonEngine(
            songOf(note(48, 0, Hand.LEFT), note(60, 0), note(62, 600)),
            HandMode.BOTH,
        )

        // Act
        val state = engine.state.value

        // Assert
        assertEquals(setOf(48, 60), state.expectedNotes)
    }
}
