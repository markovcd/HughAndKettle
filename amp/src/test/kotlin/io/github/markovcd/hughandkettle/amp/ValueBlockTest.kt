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
                    mapOf(29 to 180, 28 to 0, 4 to 221, 27 to 140, 1 to 0, 12 to 10) +
                    mapOf(64 to 0, 55 to 0, 63 to 127, 30 to 115),
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

    /** The switches and power soak a block captured on 2026-10-22 carries, each after changing one setting. */
    private fun switches(hex: String): Map<Int, Int?> =
        listOf(64, 55, 63, 30).associateWith { heard(hex)?.values?.get(it) }

    private fun captured(
        switches: String,
        flags: String,
        check: String,
    ) = "00 20 44 00 10 00 0A 00 40 7F 7F 0A 01 09 00 09 01 1E 01 66 00 4C 01 2E 00 6B 00 00 00 00 00 01 " +
        "00 00 00 00 00 00 00 00 00 00 $switches $flags $check"

    @Test
    fun `The flags byte holds boost, the FX loop and the power soak's step, and the byte ahead of it the noise gate`() {
        assertEquals(mapOf(64 to 0, 55 to 0, 63 to 0, 30 to 115), switches(captured("00", "40", "74")))
        assertEquals(mapOf(64 to 127, 55 to 0, 63 to 0, 30 to 115), switches(captured("00", "44", "70")))
        assertEquals(mapOf(64 to 0, 55 to 127, 63 to 0, 30 to 115), switches(captured("00", "48", "7C")))
        assertEquals(mapOf(64 to 0, 55 to 0, 63 to 127, 30 to 115), switches(captured("01", "40", "75")))
        assertEquals(mapOf(64 to 0, 55 to 0, 63 to 0, 30 to 89), switches(captured("00", "30", "04")))
        assertEquals(mapOf(64 to 0, 55 to 0, 63 to 0, 30 to 12), switches(captured("00", "00", "34")))
    }

    @Test
    fun `The power soak's steps read as the amp's own selector`() {
        val soak = AmpModels.grandMeisterDeluxe40.control("power_soak")!!.kind as ControlKind.Selector
        val steps = listOf("00" to "34", "10" to "24", "20" to "14", "30" to "04", "40" to "74")
        assertEquals(
            listOf("Off", "1 W", "5 W", "20 W", "40 W"),
            steps.map { (flags, check) ->
                soak.options[Sectors.index(heard(captured("00", flags, check))!!.values.getValue(30), 5)]
            },
        )
    }

    @Test
    fun `A block written for a stand-in amp keeps its switches and power soak`() {
        val block =
            heard(stored)!!.copy(
                values =
                    heard(stored)!!.values + mapOf(64 to 127, 55 to 127, 63 to 0, 30 to 38),
            )
        assertEquals(block, ValueBlock.parse(block.message()))
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
