package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModels
import kotlin.test.Test
import kotlin.test.assertEquals

class ControlValueTest {
    private val amp = AmpModels.blackSpirit200

    private fun seen(
        control: String,
        text: String,
    ): Any = ControlValue.parse(amp.control(control)!!, text).fold({ it.value }, { it.message!! })

    @Test
    fun `A knob with a scale takes an amount in its unit, and the raw value still`() {
        val delay = amp.control("delay_time")!!.scale!!
        assertEquals(delay.value(520), seen("delay_time", "520ms"))
        assertEquals(127, seen("delay_time", "1360 ms"))
        assertEquals(64, seen("delay_time", "64"))
    }

    @Test
    fun `An amount outside the scale, or on a knob without one, is refused with what it takes`() {
        assertEquals("'delay_time' takes 0..127 or 51..1360ms, got '50ms'", seen("delay_time", "50ms"))
        assertEquals("'gain' takes 0..127, got '100ms'", seen("gain", "100ms"))
    }
}
