package dev.asnodj.pianotrainer.midi

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MidiMessageParserTest {

    private val parser = MidiMessageParser()

    private fun bytes(vararg values: Int): ByteArray {
        return ByteArray(values.size) { index -> values[index].toByte() }
    }

    @Test
    fun `note on is parsed with note, velocity and channel`() {
        // Prepare
        val packet = bytes(0x90, 60, 100)

        // Act
        val events = parser.parse(packet, 0, packet.size, 42L)

        // Assert
        assertEquals(1, events.size)
        assertEquals(60, events[0].note)
        assertEquals(100, events[0].velocity)
        assertTrue(events[0].isNoteOn)
        assertEquals(0, events[0].channel)
        assertEquals(42L, events[0].timestampNanos)
    }

    @Test
    fun `note off message is parsed as note off`() {
        // Prepare
        val packet = bytes(0x81, 60, 64)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(1, events.size)
        assertFalse(events[0].isNoteOn)
        assertEquals(1, events[0].channel)
    }

    @Test
    fun `note on with velocity zero is treated as note off`() {
        // Prepare
        val packet = bytes(0x90, 60, 0)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(1, events.size)
        assertFalse(events[0].isNoteOn)
    }

    @Test
    fun `running status parses consecutive notes without repeated status byte`() {
        // Prepare
        val packet = bytes(0x90, 60, 100, 62, 100, 64, 100)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(3, events.size)
        assertEquals(listOf(60, 62, 64), events.map { event -> event.note })
        assertTrue(events.all { event -> event.isNoteOn })
    }

    @Test
    fun `real-time clock bytes are ignored and do not break running status`() {
        // Prepare: 0xF8 = MIDI clock, sent continuously by the PSR-E383.
        val packet = bytes(0x90, 0xF8, 60, 0xF8, 100, 0xF8, 62, 100)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(2, events.size)
        assertEquals(listOf(60, 62), events.map { event -> event.note })
    }

    @Test
    fun `control change messages produce no note events but keep the stream in sync`() {
        // Prepare: sustain pedal press then a note on.
        val packet = bytes(0xB0, 64, 127, 0x90, 60, 100)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(1, events.size)
        assertEquals(60, events[0].note)
    }

    @Test
    fun `message split across two packets is reassembled`() {
        // Prepare
        val firstPacket = bytes(0x90, 60)
        val secondPacket = bytes(100)

        // Act
        val firstEvents = parser.parse(firstPacket, 0, firstPacket.size, 0L)
        val secondEvents = parser.parse(secondPacket, 0, secondPacket.size, 0L)

        // Assert
        assertTrue(firstEvents.isEmpty())
        assertEquals(1, secondEvents.size)
        assertEquals(60, secondEvents[0].note)
        assertTrue(secondEvents[0].isNoteOn)
    }

    @Test
    fun `offset and count restrict parsing to the valid slice`() {
        // Prepare: valid message surrounded by garbage that must be ignored.
        val packet = bytes(0x11, 0x90, 60, 100, 0x22)

        // Act
        val events = parser.parse(packet, 1, 3, 0L)

        // Assert
        assertEquals(1, events.size)
        assertEquals(60, events[0].note)
    }

    @Test
    fun `program change with single data byte does not desynchronize following notes`() {
        // Prepare: program change (1 data byte) then note on.
        val packet = bytes(0xC0, 5, 0x90, 60, 100)

        // Act
        val events = parser.parse(packet, 0, packet.size, 0L)

        // Assert
        assertEquals(1, events.size)
        assertEquals(60, events[0].note)
    }
}
