package io.github.markovcd.hughandkettle.app

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KnobDialTest {
    @Test
    fun `The notch points at seven o'clock at 0 and at five o'clock at 127`() {
        assertEquals(135f, KnobDial.angleDegrees(0))
        assertEquals(405f, KnobDial.angleDegrees(127))
        assertEquals(270f, KnobDial.angleDegrees(64), 2.2f)
    }

    @Test
    fun `A full sweep is a 190 dp drag up or right`() {
        assertEquals(127, KnobDial.valueAfterDrag(0, dxDp = 190.5f, dyDp = 0f, fine = false))
        assertEquals(127, KnobDial.valueAfterDrag(0, dxDp = 0f, dyDp = -190.5f, fine = false))
    }

    @Test
    fun `Dragging down or left turns the knob down`() {
        assertEquals(90, KnobDial.valueAfterDrag(100, dxDp = -15f, dyDp = 0f, fine = false))
        assertEquals(90, KnobDial.valueAfterDrag(100, dxDp = 0f, dyDp = 15f, fine = false))
    }

    @Test
    fun `Fine mode needs four times the distance per step`() {
        assertEquals(10, KnobDial.valueAfterDrag(0, dxDp = 60f, dyDp = 0f, fine = true))
        assertEquals(40, KnobDial.valueAfterDrag(0, dxDp = 60f, dyDp = 0f, fine = false))
    }

    @Test
    fun `A drag past either end stops at the end`() {
        assertEquals(0, KnobDial.valueAfterDrag(10, dxDp = -500f, dyDp = 0f, fine = false))
        assertEquals(127, KnobDial.valueAfterDrag(120, dxDp = 500f, dyDp = 0f, fine = false))
    }

    @Test
    fun `A tick is felt at 0, 64 and 127 and nowhere else`() {
        assertTrue(KnobDial.reachesTick(63, 64))
        assertTrue(KnobDial.reachesTick(60, 70))
        assertTrue(KnobDial.reachesTick(1, 0))
        assertTrue(KnobDial.reachesTick(126, 127))
        assertFalse(KnobDial.reachesTick(64, 65))
        assertFalse(KnobDial.reachesTick(10, 20))
        assertFalse(KnobDial.reachesTick(64, 64))
    }
}
