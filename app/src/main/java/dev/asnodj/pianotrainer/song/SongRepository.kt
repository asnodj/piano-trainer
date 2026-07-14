package dev.asnodj.pianotrainer.song

import android.content.Context

/**
 * Loads the songs bundled in assets/songs (each one a .mid + .json sidecar pair).
 */
class SongRepository(private val context: Context) {

    private val loader = SmfSongLoader()

    /**
     * Loads every bundled song, easiest (fewest notes) first.
     * Songs that fail to parse are skipped rather than crashing the app.
     *
     * @return The playable songs.
     */
    fun loadBundledSongs(): List<Song> {
        val assetManager = context.assets
        val midiFiles = assetManager.list("songs").orEmpty().filter { fileName ->
            fileName.endsWith(".mid")
        }
        return midiFiles.mapNotNull { fileName ->
            val slug = fileName.removeSuffix(".mid")
            runCatching {
                val midiBytes = assetManager.open("songs/$fileName").use { stream -> stream.readBytes() }
                val metadataJson = assetManager.open("songs/$slug.json").use { stream ->
                    stream.bufferedReader().readText()
                }
                loader.load(slug, midiBytes, metadataJson)
            }.getOrNull()
        }.sortedBy { song -> song.notes.size }
    }
}
