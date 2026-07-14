package dev.asnodj.pianotrainer.song

/** Which hand plays a note. */
enum class Hand {
    LEFT,
    RIGHT;

    companion object {
        /**
         * Parses a sidecar-metadata hand key.
         *
         * @param key "left" or "right" (case-insensitive).
         * @return The matching [Hand]; defaults to [RIGHT] for unknown values.
         */
        fun fromKey(key: String): Hand {
            return if (key.equals("left", ignoreCase = true)) LEFT else RIGHT
        }
    }
}

/**
 * One playable note of a song, in absolute milliseconds from the song start.
 *
 * @property midiNote MIDI note number.
 * @property startMs When the note starts, in ms from the beginning of the song.
 * @property durationMs How long the note is held.
 * @property hand Which hand plays it.
 * @property finger Fingering 1 (thumb) .. 5 (pinky), or null when not authored.
 */
data class SongNote(
    val midiNote: Int,
    val startMs: Int,
    val durationMs: Int,
    val hand: Hand,
    val finger: Int?,
)

/**
 * A practice chunk of a few bars (the loop/repeat unit of the learning flow).
 *
 * @property index Zero-based position of the section in the song.
 * @property startMs Inclusive start of the section.
 * @property endMs Exclusive end of the section.
 */
data class Section(
    val index: Int,
    val startMs: Int,
    val endMs: Int,
)

/**
 * A fully loaded song, ready for the lesson/tempo engines.
 *
 * @property id Stable identifier (asset slug).
 * @property title Display title.
 * @property notes All notes sorted by start time (then pitch).
 * @property sections Practice sections covering the whole song.
 * @property durationMs Total play time.
 */
data class Song(
    val id: String,
    val title: String,
    val notes: List<SongNote>,
    val sections: List<Section>,
    val durationMs: Int,
)
