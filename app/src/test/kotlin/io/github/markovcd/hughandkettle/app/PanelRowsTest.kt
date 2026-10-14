package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelRowsTest {
    @Test
    fun `The preamp's four knobs share one row`() {
        val preamp = AmpModels.grandMeisterDeluxe40.controls.filter { it.section == Section.Preamp }
        val rows = PanelRows.of(preamp).map { row -> row.map(Control::id) }
        assertEquals(listOf(listOf("gain", "bass", "mid", "treble")), rows)
    }

    @Test
    fun `Knobs fill rows of four`() {
        val knobs = List(7) { Control("k$it", "K$it", it, ControlKind.Knob, Section.Preamp) }
        assertEquals(listOf(4, 3), PanelRows.of(knobs).map { it.size })
    }

    @Test
    fun `A switch between knobs closes the knob row before it`() {
        val fx = AmpModels.blackSpirit200.controls.filter { it.section == Section.Fx }
        val rows = PanelRows.of(fx)
        rows.filter { it.size > 1 }.forEach { row -> assertEquals(true, row.all { it.kind == ControlKind.Knob }) }
        assertEquals(listOf("modulation"), rows.first().map(Control::id))
        assertEquals(listOf("modulation_intensity", "modulation_speed"), rows[2].map(Control::id))
    }
}
