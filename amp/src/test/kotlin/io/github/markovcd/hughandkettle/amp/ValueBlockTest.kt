package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.SystemExclusive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ValueBlockTest {
    /** A block as the amp sent it, F0 and F7 left off. */
    private fun heard(hex: String): ValueBlock? =
        ValueBlock.parse(SystemExclusive(hex.split(' ').map { it.toInt(16).toByte() }.toByteArray()))

    private val stored =
        "00 20 44 00 10 00 0A 00 40 00 00 7C 00 1E 00 14 00 28 00 32 00 3C 00 50 00 7F " +
            "01 34 00 00 01 5D 01 0C 00 00 00 0A 00 00 00 00 01 40 1E"

    @Test
    fun `A block captured after a store reads as the preset and its knob values`() {
        val expected =
            ValueBlock(
                mapOf(56 to 30, 21 to 20, 22 to 40, 57 to 50, 23 to 60, 24 to 80, 25 to 127) +
                    mapOf(29 to 180, 28 to 0, 4 to 221, 27 to 140, 1 to 0, 12 to 10),
                preset = 125,
            )
        assertEquals(expected, heard(stored))
    }

    @Test
    fun `A block captured after a channel switch names no preset`() {
        val switched =
            "00 20 44 00 10 00 0A 00 40 7F 7F 01 00 03 00 6A 01 06 01 3A 00 74 00 1A 01 01 " +
                "01 34 01 20 01 5D 01 0C 01 49 00 0A 00 00 00 00 01 41 33"
        assertEquals(null, heard(switched)?.preset)
        assertEquals(3, heard(switched)?.values?.get(56))
        assertEquals(1, heard(switched)?.channel)
    }

    @Test
    fun `The flags byte's low two bits name the channel`() {
        assertEquals(0, heard(stored)?.channel)
        val ultra = heard(stored)!!.copy(preset = null, channel = 3)
        assertEquals(ultra, ValueBlock.parse(ultra.message()))
    }

    @Test
    fun `A block whose check does not match is no block`() {
        assertNull(heard(stored.dropLast(2) + "1F"))
    }

    @Test
    fun `A control report is no block`() {
        assertNull(heard("00 20 44 00 10 00 0A 00 04 09 00 7F 7C"))
    }

    @Test
    fun `A block written for a stand-in amp reads back as itself`() {
        val block = heard(stored)!!
        assertEquals(block, ValueBlock.parse(block.message()))
        assertEquals(block.copy(preset = null), ValueBlock.parse(block.copy(preset = null).message()))
    }
}
