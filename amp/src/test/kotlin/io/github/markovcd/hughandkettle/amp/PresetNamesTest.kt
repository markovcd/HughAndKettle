package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PresetNamesTest {
    private val amp = AmpModels.grandMeisterDeluxe40

    @Test
    fun `A preset is named trimmed and unnamed by a blank name`() {
        val named = PresetNames(amp).with(12, "  Lead solo ")
        assertEquals("Lead solo", named[12])
        assertNull(named.with(12, " ")[12])
    }

    @Test
    fun `A tone read from a preset is called by its number and then its name`() {
        val names = PresetNames(amp).with(12, "Lead solo")
        assertEquals("12 Lead solo", names.toneName(12))
        assertEquals("Preset 13", names.toneName(13))
    }

    @Test
    fun `A preset the amp does not have cannot be named`() {
        assertFailsWith<IllegalArgumentException> { PresetNames(amp).with(amp.presetCount + 1, "Lost") }
        assertFailsWith<IllegalArgumentException> { PresetNames(amp, mapOf(0 to "Lost")) }
    }
}
