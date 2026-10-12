package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class ScaleTest {
    private val delay = AmpModels.blackSpirit200.control("delay_time")!!.scale!!

    @Test
    fun `Delay time runs from 51 ms at 0 to 1360 ms at 127`() {
        assertEquals(listOf(51, 1360), listOf(delay.amount(0), delay.amount(127)))
    }

    @Test
    fun `Every value reads back from its own amount`() {
        val wrong = (0..127).filter { delay.value(delay.amount(it)) != it }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun `An amount past either end is that end`() {
        assertEquals(listOf(0, 127), listOf(delay.value(10), delay.value(5000)))
    }

    @Test
    fun `Every scaled control of every amp is a knob`() {
        val wrong = AmpModels.all.flatMap { it.controls }.filter { it.scale != null && it.kind != ControlKind.Knob }
        assertEquals(emptyList(), wrong)
    }
}
