package dev.asnodj.pianotrainer.midi

/**
 * Incremental parser for a raw MIDI 1.0 byte stream.
 *
 * Pure Kotlin (no Android dependency) so it can be unit-tested on the JVM.
 * Handles running status, note-on with velocity 0 (treated as note-off, as sent
 * by many keyboards), and ignores system real-time bytes (e.g. the MIDI clock
 * the PSR-E383 sends continuously) without breaking running status.
 *
 * Stateful: keep one instance per MIDI stream and feed it every packet in order.
 */
class MidiMessageParser {

    private var runningStatus = 0
    private val pendingDataBytes = IntArray(2)
    private var pendingDataCount = 0

    /**
     * Parses one packet of the MIDI stream and returns the note events it completes.
     *
     * @param data Raw bytes as delivered by [android.media.midi.MidiReceiver.onSend].
     * @param offset Index of the first valid byte in [data].
     * @param count Number of valid bytes starting at [offset].
     * @param timestampNanos Packet timestamp (System.nanoTime() base) applied to all
     *   events completed by this packet.
     * @return Note-on/note-off events completed by this packet, in stream order.
     *   Non-note channel messages (control change, program change…) are consumed
     *   silently to keep the stream in sync.
     */
    fun parse(data: ByteArray, offset: Int, count: Int, timestampNanos: Long): List<MidiNoteEvent> {
        val completedEvents = mutableListOf<MidiNoteEvent>()
        for (byteIndex in offset until offset + count) {
            val currentByte = data[byteIndex].toInt() and 0xFF
            when {
                // System real-time (0xF8..0xFF): may appear anywhere, never alters running status.
                currentByte >= 0xF8 -> Unit

                // System common (0xF0..0xF7): cancels running status; payload bytes are
                // then skipped because no running status is active.
                currentByte >= 0xF0 -> {
                    runningStatus = 0
                    pendingDataCount = 0
                }

                // Channel message status byte: becomes the new running status.
                currentByte >= 0x80 -> {
                    runningStatus = currentByte
                    pendingDataCount = 0
                }

                // Data byte: accumulate until the current message is complete.
                else -> {
                    if (runningStatus == 0) {
                        continue
                    }
                    pendingDataBytes[pendingDataCount] = currentByte
                    pendingDataCount++
                    if (pendingDataCount == expectedDataByteCount(runningStatus)) {
                        buildNoteEvent(timestampNanos)?.let(completedEvents::add)
                        // Running status persists: the next data byte starts a new message.
                        pendingDataCount = 0
                    }
                }
            }
        }
        return completedEvents
    }

    /**
     * Returns how many data bytes the given channel status byte expects.
     *
     * @param statusByte A channel status byte in 0x80..0xEF.
     * @return 1 for program change / channel pressure, 2 for everything else.
     */
    private fun expectedDataByteCount(statusByte: Int): Int {
        return when (statusByte and 0xF0) {
            0xC0, 0xD0 -> 1
            else -> 2
        }
    }

    /**
     * Builds a [MidiNoteEvent] from the completed message, or null when the message
     * is not a note-on/note-off.
     *
     * @param timestampNanos Timestamp to stamp on the event.
     * @return The note event, or null for non-note channel messages.
     */
    private fun buildNoteEvent(timestampNanos: Long): MidiNoteEvent? {
        val channel = runningStatus and 0x0F
        return when (runningStatus and 0xF0) {
            0x90 -> {
                val velocity = pendingDataBytes[1]
                MidiNoteEvent(
                    note = pendingDataBytes[0],
                    velocity = velocity,
                    // Note-on with velocity 0 is the standard way to encode note-off.
                    isNoteOn = velocity > 0,
                    channel = channel,
                    timestampNanos = timestampNanos,
                )
            }
            0x80 -> MidiNoteEvent(
                note = pendingDataBytes[0],
                velocity = 0,
                isNoteOn = false,
                channel = channel,
                timestampNanos = timestampNanos,
            )
            else -> null
        }
    }
}
