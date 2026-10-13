package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelRowsTest {
    @Test
    fun `A group of knobs that does not fit what is left of a row starts the next one`() {
        val amp = AmpModels.blackSpirit200.controls.filter { it.section == Section.Amp }
        val rows = PanelRows.of(amp).map { row -> row.map(Control::id) }
        assertEquals(
            listOf(listOf("gain", "volume"), listOf("bass", "mid", "treble"), listOf("resonance", "presence")),
            rows,
        )
    }

    @Test
    fun `Knobs with no groups fill rows of three`() {
        val knobs = List(7) { Control("k$it", "K$it", it, ControlKind.Knob, Section.Amp) }
        assertEquals(listOf(3, 3, 1), PanelRows.of(knobs).map { it.size })
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
