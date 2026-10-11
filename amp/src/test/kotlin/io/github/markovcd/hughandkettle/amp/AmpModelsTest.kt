package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AmpModelsTest {
    @Test
    fun `Every amp has a distinct id and every control a distinct controller`() {
        val ids = AmpModels.all.map(AmpModel::id)
        assertEquals(ids.size, ids.toSet().size)
        AmpModels.all.forEach { amp ->
            val controllers = amp.controls.map(Control::controller)
            assertEquals(controllers.size, controllers.toSet().size, amp.name)
        }
    }

    @Test
    fun `The Black Spirit 200 chart matches the manual`() {
        val chart = AmpModels.blackSpirit200.controls.associate { it.id to it.controller }
        val fromManual =
            mapOf(
                "gain" to 20,
                "channel" to 31,
                "cabinet" to 58,
                "sagging" to 59,
                "noise_gate_sensitivity" to 62,
                "boost" to 64,
            )
        assertEquals(fromManual, chart.filterKeys { it in fromManual })
        assertTrue("power_soak" !in chart)
    }

    @Test
    fun `The GrandMeister Deluxe 40 has a power soak and no cabinet`() {
        val chart = AmpModels.grandMeisterDeluxe40.controls.associate { it.id to it.controller }
        assertEquals(30, chart["power_soak"])
        assertTrue("cabinet" !in chart)
    }

    @Test
    fun `Setting a knob sends its controller on the given channel`() {
        val gain = AmpModels.blackSpirit200.control("gain")!!
        assertEquals(ControlChange(MidiChannel(2), 20, 100), gain.set(100, MidiChannel(2)))
    }

    @Test
    fun `Selecting the Lead channel lands in the third sector`() {
        val channel = AmpModels.blackSpirit200.control("channel")!!
        assertEquals(ControlChange(MidiChannel.Default, 31, 79), channel.selected(2))
    }

    @Test
    fun `A switch sends 127 for on and 0 for off`() {
        val boost = AmpModels.blackSpirit200.control("boost")!!
        assertEquals(127, boost.switched(true).value)
        assertEquals(0, boost.switched(false).value)
    }

    @Test
    fun `Every section of every amp holds at least one control, and the channel selector opens Switching`() {
        AmpModels.all.forEach { amp ->
            val bySection = amp.controls.groupBy(Control::section)
            assertEquals(Section.entries.toSet(), bySection.keys, amp.name)
            assertEquals("channel", bySection.getValue(Section.Switching).first().id, amp.name)
        }
    }
}
