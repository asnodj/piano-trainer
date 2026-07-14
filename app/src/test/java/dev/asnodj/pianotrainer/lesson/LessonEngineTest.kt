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
    fun `accuracy stays at 100 percent without any wrong press`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(60)
        engine.onNoteOn(62)

        // Assert
        assertEquals(100, engine.state.value.accuracyPercent)
        assertEquals(0, engine.state.value.wrongPressCount)
    }

    @Test
    fun `wrong presses lower the accuracy and survive group advances`() {
        // Prepare: 2 notes; 2 wrong presses -> 2 / (2 + 2) = 50%.
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600)), HandMode.BOTH)

        // Act
        engine.onNoteOn(65)
        engine.onNoteOn(60)
        engine.onNoteOn(66)
        engine.onNoteOn(62)

        // Assert
        val state = engine.state.value
        assertTrue(state.finished)
        assertEquals(2, state.wrongPressCount)
        assertEquals(50, state.accuracyPercent)
    }

    @Test
    fun `seeking jumps to the target group and keeps the accumulated score`() {
        // Prepare
        val engine = LessonEngine(
            songOf(note(60, 0), note(62, 600), note(64, 1200), note(65, 1800)),
            HandMode.BOTH,
        )
        engine.onNoteOn(99)

        // Act
        engine.seekToFraction(0.5f)

        // Assert
        assertEquals(2, engine.state.value.groupIndex)
        assertEquals(setOf(64), engine.state.value.expectedNotes)
        assertEquals(1, engine.state.value.wrongPressCount)

        // Act: rewind to the beginning.
        engine.seekToGroup(0)

        // Assert
        assertEquals(0, engine.state.value.groupIndex)
        assertFalse(engine.state.value.finished)
    }

    @Test
    fun `seeking by song position lands on the first group at or after it`() {
        // Prepare
        val engine = LessonEngine(songOf(note(60, 0), note(62, 600), note(64, 1200)), HandMode.BOTH)

        // Act
        engine.seekToMs(600)

        // Assert
        assertEquals(1, engine.state.value.groupIndex)
        assertEquals(600, engine.state.value.positionMs)
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
