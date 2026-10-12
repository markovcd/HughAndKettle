package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import kotlin.test.Test
import kotlin.test.assertEquals

class PanelRowsTest {
    @Test
    fun `Knobs fill rows of three and a switch or selector gets a row of its own`() {
        val amp = AmpModels.blackSpirit200.controls.filter { it.section == Section.Amp }
        val rows = PanelRows.of(amp).map { row -> row.map(Control::id) }
        assertEquals(
            listOf(listOf("gain", "bass", "mid"), listOf("treble", "resonance", "presence"), listOf("volume")),
            rows,
        )
    }

    @Test
    fun `A switch between knobs closes the knob row before it`() {
        val fx = AmpModels.blackSpirit200.controls.filter { it.section == Section.Fx }
        val rows = PanelRows.of(fx)
        rows.filter { it.size > 1 }.forEach { row -> assertEquals(true, row.all { it.kind == ControlKind.Knob }) }
        assertEquals(listOf("modulation"), rows.first().map(Control::id))
        assertEquals(listOf("modulation_intensity", "modulation_speed"), rows[2].map(Control::id))
    }

    @Test
    fun `On a wide panel knobs fill rows of six`() {
        val amp = AmpModels.blackSpirit200.controls.filter { it.section == Section.Amp }
        assertEquals(listOf(6, 1), PanelRows.of(amp, knobsPerRow = 6).map { it.size })
    }
}
