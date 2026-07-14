package dev.asnodj.pianotrainer.song

import kotlinx.serialization.Serializable

/**
 * Sidecar metadata authored next to each bundled .mid asset. Carries what the
 * MIDI format cannot express: hand assignment per track, fingering, section
 * size and license info.
 *
 * @property title Display title of the song.
 * @property bpm Authored tempo, informational only (the .mid tempo events rule).
 * @property barsPerSection Section (practice-loop) size in bars.
 * @property trackHands Hand of each note-bearing MIDI track, in file order
 *   ("right"/"left"). Tracks beyond this list default to right hand.
 * @property fingering Finger numbers (1..5) aligned with the song's notes
 *   sorted by (start time, pitch); null or size mismatch = no fingering.
 * @property license Human-readable license/attribution of the encoding.
 * @property source Where the file came from, for traceability.
 */
@Serializable
data class SongMetadata(
    val title: String,
    val bpm: Int? = null,
    val barsPerSection: Int = 4,
    val trackHands: List<String> = listOf("right"),
    val fingering: List<Int>? = null,
    val license: String? = null,
    val source: String? = null,
)
