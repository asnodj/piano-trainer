package dev.asnodj.pianotrainer.song

import dev.atsushieno.ktmidi.Midi1CompoundMessage
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.read
import kotlinx.serialization.json.Json

/**
 * Loads a bundled song: a Standard MIDI File plus its .json sidecar metadata.
 * Pure Kotlin/JVM (no Android dependency) so it is unit-testable.
 */
class SmfSongLoader {

    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Parses a .mid file and its sidecar into a [Song].
     *
     * @param songId Stable identifier for the song (asset slug).
     * @param midiBytes Raw content of the .mid file.
     * @param metadataJson Content of the sidecar .json file.
     * @return The song with notes in (start time, pitch) order and sections
     *   computed from the file's time signature and the sidecar's barsPerSection.
     */
    fun load(songId: String, midiBytes: ByteArray, metadataJson: String): Song {
        val metadata = json.decodeFromString<SongMetadata>(metadataJson)
        val music = Midi1Music()
        music.read(midiBytes.toList())

        val notes = extractNotes(music, metadata)
        val durationMs = music.getTotalPlayTimeMilliseconds()
        return Song(
            id = songId,
            title = metadata.title,
            notes = applyFingering(notes, metadata),
            sections = buildSections(music, metadata.barsPerSection, durationMs),
            durationMs = durationMs,
        )
    }

    /**
     * Extracts all notes of the file, assigning hands per note-bearing track
     * (in file order) from the sidecar's trackHands list.
     *
     * @param music Parsed MIDI music.
     * @param metadata Sidecar metadata.
     * @return Notes sorted by (start time, pitch), without fingering yet.
     */
    private fun extractNotes(music: Midi1Music, metadata: SongMetadata): List<SongNote> {
        val notes = mutableListOf<SongNote>()
        var noteBearingTrackIndex = 0
        music.tracks.forEach { track ->
            val pendingOnsets = mutableMapOf<Int, Int>()
            val trackNotes = mutableListOf<Pair<Int, Int>>()
            var absoluteTick = 0
            track.events.forEach { event ->
                absoluteTick += event.deltaTime
                val message = event.message
                val status = message.statusCode.toInt() and 0xFF
                val note = message.msb.toInt() and 0x7F
                val velocity = message.lsb.toInt() and 0x7F
                when {
                    status == 0x90 && velocity > 0 -> pendingOnsets[note] = absoluteTick
                    status == 0x80 || (status == 0x90 && velocity == 0) -> {
                        val onsetTick = pendingOnsets.remove(note)
                        if (onsetTick != null) {
                            trackNotes.add(onsetTick to note)
                            notes.add(buildNote(music, onsetTick, absoluteTick, note, handOf(metadata, noteBearingTrackIndex)))
                        }
                    }
                }
            }
            // Notes still held at end of track are closed at the last tick.
            pendingOnsets.forEach { (note, onsetTick) ->
                notes.add(buildNote(music, onsetTick, absoluteTick, note, handOf(metadata, noteBearingTrackIndex)))
            }
            if (trackNotes.isNotEmpty() || pendingOnsets.isNotEmpty()) {
                noteBearingTrackIndex++
            }
        }
        return notes.sortedWith(compareBy({ songNote -> songNote.startMs }, { songNote -> songNote.midiNote }))
    }

    /**
     * Builds a [SongNote] from tick boundaries, converting through the tempo map.
     *
     * @param music Parsed MIDI music (owns the tempo map).
     * @param onsetTick Absolute tick of the note-on.
     * @param releaseTick Absolute tick of the note-off.
     * @param note MIDI note number.
     * @param hand Hand playing this track.
     * @return The note with millisecond timing.
     */
    private fun buildNote(music: Midi1Music, onsetTick: Int, releaseTick: Int, note: Int, hand: Hand): SongNote {
        val startMs = music.getTimePositionInMillisecondsForTick(onsetTick)
        val endMs = music.getTimePositionInMillisecondsForTick(releaseTick)
        return SongNote(
            midiNote = note,
            startMs = startMs,
            durationMs = endMs - startMs,
            hand = hand,
            finger = null,
        )
    }

    /**
     * Resolves the hand of the Nth note-bearing track from the sidecar.
     *
     * @param metadata Sidecar metadata.
     * @param noteBearingTrackIndex Index among tracks that contain notes.
     * @return The configured hand, defaulting to right.
     */
    private fun handOf(metadata: SongMetadata, noteBearingTrackIndex: Int): Hand {
        val key = metadata.trackHands.getOrNull(noteBearingTrackIndex) ?: return Hand.RIGHT
        return Hand.fromKey(key)
    }

    /**
     * Applies the sidecar fingering list to the sorted notes. The list may be
     * shorter than the song (partially fingered pieces like Für Elise); 0 or
     * any value outside 1..5 means "no fingering" for that note.
     *
     * @param notes Notes sorted by (start time, pitch).
     * @param metadata Sidecar metadata.
     * @return Notes with fingering applied where authored.
     */
    private fun applyFingering(notes: List<SongNote>, metadata: SongMetadata): List<SongNote> {
        val fingering = metadata.fingering ?: return notes
        if (fingering.isEmpty()) {
            return notes
        }
        return notes.mapIndexed { noteIndex, songNote ->
            val finger = fingering.getOrNull(noteIndex)
            if (finger != null && finger in 1..5) songNote.copy(finger = finger) else songNote
        }
    }

    /**
     * Splits the song into fixed-size sections of barsPerSection bars, using the
     * file's first time-signature meta event (defaults to 4/4).
     *
     * @param music Parsed MIDI music.
     * @param barsPerSection Section size in bars.
     * @param durationMs Total song duration used to clamp the last section.
     * @return Contiguous sections covering the whole song.
     */
    private fun buildSections(music: Midi1Music, barsPerSection: Int, durationMs: Int): List<Section> {
        val (numerator, denominator) = readTimeSignature(music)
        val ticksPerBar = (numerator * 4.0 / denominator * music.deltaTimeSpec).toInt()
        val ticksPerSection = ticksPerBar * barsPerSection
        val totalTicks = music.getTotalTicks()

        val sections = mutableListOf<Section>()
        var sectionStartTick = 0
        var sectionIndex = 0
        while (sectionStartTick < totalTicks) {
            val sectionEndTick = minOf(sectionStartTick + ticksPerSection, totalTicks)
            sections.add(
                Section(
                    index = sectionIndex,
                    startMs = music.getTimePositionInMillisecondsForTick(sectionStartTick),
                    endMs = minOf(music.getTimePositionInMillisecondsForTick(sectionEndTick), durationMs),
                )
            )
            sectionStartTick = sectionEndTick
            sectionIndex++
        }
        return sections
    }

    /**
     * Reads the first time-signature meta event of the file.
     *
     * @param music Parsed MIDI music.
     * @return numerator to denominator (e.g. 3 to 8), defaulting to 4/4.
     */
    private fun readTimeSignature(music: Midi1Music): Pair<Int, Int> {
        music.tracks.forEach { track ->
            track.events.forEach { event ->
                val message = event.message
                if ((message.statusByte.toInt() and 0xFF) == 0xFF &&
                    (message.metaType.toInt() and 0xFF) == 0x58 &&
                    message is Midi1CompoundMessage
                ) {
                    val data = message.extraData
                    if (data != null && message.extraDataLength >= 2) {
                        val numerator = data[message.extraDataOffset].toInt()
                        val denominator = 1 shl data[message.extraDataOffset + 1].toInt()
                        return numerator to denominator
                    }
                }
            }
        }
        return 4 to 4
    }
}
