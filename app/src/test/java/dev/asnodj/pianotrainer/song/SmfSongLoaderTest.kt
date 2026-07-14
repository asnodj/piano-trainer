package dev.asnodj.pianotrainer.song

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SmfSongLoaderTest {

    private val loader = SmfSongLoader()

    /**
     * Asserts a millisecond timing with a small tolerance: ktmidi's tick-to-ms
     * conversion truncates, which is harmless for gameplay.
     */
    private fun assertNearMs(expectedMs: Int, actualMs: Int) {
        assertTrue(
            "expected ~${expectedMs}ms but was ${actualMs}ms",
            kotlin.math.abs(expectedMs - actualMs) <= 5,
        )
    }

    private fun loadAsset(slug: String): Song {
        // Unit tests run with the module directory as working directory.
        val songsDir = File("src/main/assets/songs")
        return loader.load(
            songId = slug,
            midiBytes = File(songsDir, "$slug.mid").readBytes(),
            metadataJson = File(songsDir, "$slug.json").readText(),
        )
    }

    @Test
    fun `au clair de la lune loads with all authored notes`() {
        // Prepare / Act
        val song = loadAsset("au_clair_de_la_lune")

        // Assert: 44 melody notes + 16 bass whole notes.
        assertEquals("Au clair de la lune", song.title)
        assertEquals(60, song.notes.size)
    }

    @Test
    fun `au clair de la lune melody starts with three C4 quarters at 100 bpm`() {
        // Prepare / Act
        val song = loadAsset("au_clair_de_la_lune")

        // Assert
        val melody = song.notes.filter { songNote -> songNote.hand == Hand.RIGHT }.take(4)
        assertEquals(listOf(60, 60, 60, 62), melody.map { songNote -> songNote.midiNote })
        assertEquals(0, melody[0].startMs)
        // Quarter note at 100 bpm = 600 ms.
        assertNearMs(600, melody[1].startMs)
        assertNearMs(1200, melody[2].startMs)
    }

    @Test
    fun `au clair de la lune is chunked into four 4-bar sections`() {
        // Prepare / Act
        val song = loadAsset("au_clair_de_la_lune")

        // Assert: 16 bars of 4/4 at 100 bpm -> 4 sections of 9600 ms.
        assertEquals(4, song.sections.size)
        assertEquals(0, song.sections[0].startMs)
        assertNearMs(9600, song.sections[0].endMs)
        assertNearMs(9600, song.sections[1].startMs)
    }

    @Test
    fun `au clair de la lune carries authored fingering on every note of both hands`() {
        // Prepare / Act
        val song = loadAsset("au_clair_de_la_lune")

        // Assert: the generated sidecar fingers all 60 notes; at t=0 the notes
        // sorted by pitch are the LH C3 bass (finger 2) then the RH C4 (thumb).
        assertTrue(song.notes.all { songNote -> songNote.finger != null })
        assertEquals(48, song.notes[0].midiNote)
        assertEquals(2, song.notes[0].finger)
        assertEquals(60, song.notes[1].midiNote)
        assertEquals(1, song.notes[1].finger)
    }

    @Test
    fun `au clair de la lune now has a simplified left hand`() {
        // Prepare / Act
        val song = loadAsset("au_clair_de_la_lune")

        // Assert: 16 bass whole notes in a stationary G2..D3 position.
        val bass = song.notes.filter { songNote -> songNote.hand == Hand.LEFT }
        assertEquals(16, bass.size)
        assertTrue(bass.all { songNote -> songNote.midiNote in 43..50 })
    }

    @Test
    fun `fur elise loads with both hands from its two note tracks`() {
        // Prepare / Act
        val song = loadAsset("fur_elise")

        // Assert
        assertTrue(song.notes.size > 500)
        assertTrue(song.notes.any { songNote -> songNote.hand == Hand.LEFT })
        assertTrue(song.notes.any { songNote -> songNote.hand == Hand.RIGHT })
        assertTrue(song.durationMs > 60_000)
        assertTrue(song.sections.size > 5)
    }

    @Test
    fun `fur elise opens with the famous E - D sharp alternation in the right hand`() {
        // Prepare / Act
        val song = loadAsset("fur_elise")

        // Assert: E5 (76) then D#5 (75).
        val firstMelodyNotes = song.notes.filter { songNote -> songNote.hand == Hand.RIGHT }.take(2)
        assertEquals(listOf(76, 75), firstMelodyNotes.map { songNote -> songNote.midiNote })
        assertNotNull(song.notes.first().durationMs)
    }

    @Test
    fun `fur elise opening theme is fingered but the rest is not (partial fingering)`() {
        // Prepare / Act
        val song = loadAsset("fur_elise")

        // Assert: classic 4-3 alternation on the opening E/D#, nothing at note 100.
        assertEquals(4, song.notes[0].finger)
        assertEquals(3, song.notes[1].finger)
        assertTrue(song.notes[100].finger == null)
    }
}
