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
                "gain" to 56,
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
    fun `The GrandMeister Deluxe 40 power soak steps run up from off as the amp reports them`() {
        val soak = AmpModels.grandMeisterDeluxe40.control("power_soak")!!.kind as ControlKind.Selector
        val heard = listOf(0, 32, 64, 95).map { soak.options[Sectors.index(it, soak.options.size)] }
        assertEquals(listOf("Off", "1 W", "5 W", "20 W"), heard)
        assertEquals("40 W", soak.options.last())
    }

    @Test
    fun `The GrandMeister 36 chart matches the manual`() {
        val chart = AmpModels.grandMeister36.controls.associate { it.id to it.controller }
        val fromManual =
            mapOf(
                "modulation_intensity" to 1,
                "delay_time" to 4,
                "mute" to 9,
                "power_soak" to 30,
                "channel" to 31,
                "volume" to 57,
                "noise_gate" to 63,
                "boost" to 64,
            )
        assertEquals(fromManual, chart.filterKeys { it in fromManual })
        // The manual's soft Gain and Volume, 20 and 7, are left out for the hard ones.
        val manualControllers =
            listOf(1, 4, 9, 12, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 52, 53, 54, 55, 56, 57, 63, 64)
        assertEquals(manualControllers, chart.values.sorted())
        assertEquals(PresetSlot(32, 4), AmpModels.grandMeister36.slotOf(128))
    }

    @Test
    fun `Gain and Volume are the controllers the amp reports its own knobs on`() {
        AmpModels.all.forEach { amp ->
            assertEquals(56, amp.control("gain")?.controller, amp.name)
            assertEquals(57, amp.control("volume")?.controller, amp.name)
            assertTrue(amp.controls.none { it.controller == 20 || it.controller == 7 }, amp.name)
        }
    }

    @Test
    fun `Setting a knob sends its controller on the given channel`() {
        val gain = AmpModels.blackSpirit200.control("gain")!!
        assertEquals(ControlChange(MidiChannel(2), 56, 100), gain.set(100, MidiChannel(2)))
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

    @Test
    fun `Every effect control but a switch needs the switch of its effect`() {
        // The FX loop is a switch of its own with no knobs behind it.
        val loose =
            AmpModels.all.flatMap { amp ->
                amp.controls
                    .filter { it.section == Section.Fx && it.kind != ControlKind.Switch }
                    .filter { control -> control.needs == null || !control.id.startsWith(control.needs) }
                    .map { "${amp.id}/${it.id} needs ${it.needs}" }
            }
        assertEquals(emptyList(), loose.take(5))
    }

    @Test
    fun `A control that needs something other than a switch is refused`() {
        val gain = Control("gain", "Gain", 20, ControlKind.Knob, Section.Preamp)
        val failure =
            runCatching {
                AmpModel(
                    "test",
                    "Test",
                    listOf(gain, Control("bass", "Bass", 21, ControlKind.Knob, Section.Preamp, needs = "gain")),
                )
            }.exceptionOrNull()
        assertEquals("Test: 'bass' needs 'gain', which is not one of its switches", failure?.message)
    }
}
