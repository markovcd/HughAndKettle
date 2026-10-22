package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import io.github.markovcd.hughandkettle.midi.SystemRealtime
import kotlin.test.Test
import kotlin.test.assertEquals

class AmpStateTest {
    private val amp = AmpModels.blackSpirit200
    private val gain = amp.control("gain")!!
    private val start = AmpState(amp)

    @Test
    fun `A control change sets its control and nothing else`() {
        assertEquals(mapOf("gain" to 100), start.after(gain.set(100)).values)
    }

    @Test
    fun `A later control change replaces the earlier value`() {
        assertEquals(mapOf("gain" to 3), start.after(gain.set(100)).after(gain.set(3)).values)
    }

    @Test
    fun `A controller the amp does not have changes nothing`() {
        assertEquals(start, start.after(ControlChange(MidiChannel.Default, 119, 5)))
    }

    @Test
    fun `A program change recalls the preset counted from 1 and forgets every value`() {
        val state = start.after(gain.set(100)).after(amp.recall(5))
        assertEquals(AmpState(amp, emptyMap(), preset = 5), state)
    }

    @Test
    fun `A program change past the amp's presets changes nothing`() {
        val small = AmpModel("small", "Small", amp.controls, presetCount = 4)
        val state = AmpState(small).after(ProgramChange(MidiChannel.Default, 4))
        assertEquals(AmpState(small), state)
    }

    private fun report(
        controller: Int,
        value: Int,
    ): SystemExclusive {
        val (high, low) = value shr 7 to (value and 0x7F)
        val header = listOf(0x00, 0x20, 0x44, 0x00, 0x10, 0x00, 0x0A, 0x00, 0x04)
        val payload = header + listOf(controller, high, low, 0x0A xor controller xor high xor low)
        return SystemExclusive(payload.map { it.toByte() }.toByteArray())
    }

    @Test
    fun `A knob the amp reports in 256 steps lands on 0 to 127`() {
        assertEquals(mapOf("bass" to 103), start.after(report(21, 206)).values)
    }

    @Test
    fun `A switch and a selector the amp reports keep their value`() {
        val grandMeister = AmpState(AmpModels.grandMeisterDeluxe40)
        assertEquals(mapOf("boost" to 127), start.after(report(64, 127)).values)
        assertEquals(mapOf("power_soak" to 95), grandMeister.after(report(30, 95)).values)
    }

    @Test
    fun `The modulation type is reported in 256 steps like a knob`() {
        assertEquals(mapOf("modulation_type" to 127), start.after(report(12, 255)).values)
    }

    @Test
    fun `The amp's block after a recall fills in the preset's knobs`() {
        val grandMeister = AmpModels.grandMeisterDeluxe40
        val payload =
            "00 20 44 00 10 00 0A 00 40 00 00 7C 00 1E 00 14 00 28 00 32 00 3C 00 50 00 7F " +
                "01 34 00 00 01 5D 01 0C 00 00 00 0A 00 00 00 00 01 40 1E"
        val block = SystemExclusive(payload.split(' ').map { it.toInt(16).toByte() }.toByteArray())
        val state = AmpState(grandMeister).after(grandMeister.recall(125)).after(block)
        assertEquals(125, state.preset)
        assertEquals(
            mapOf("gain" to 15, "bass" to 10, "presence" to 63, "delay_time" to 110),
            state.values.filterKeys {
                it in
                    setOf("gain", "bass", "presence", "delay_time")
            },
        )
    }

    @Test
    fun `The amp's block sets the channel, the switches and the power soak it carries`() {
        val grandMeister = AmpModels.grandMeisterDeluxe40
        val ultra = ValueBlock(mapOf(64 to 127, 55 to 0, 63 to 127, 30 to 89), preset = 2, channel = 3).message()
        val state = AmpState(grandMeister).after(ultra)
        assertEquals(
            mapOf("channel" to 111, "boost" to 127, "fx_loop" to 0, "noise_gate" to 127, "power_soak" to 89),
            state.values.filterKeys { it in setOf("channel", "boost", "fx_loop", "noise_gate", "power_soak") },
        )
    }

    @Test
    fun `After a recall an effect the amp did not report is on when its level in the block is above 0`() {
        val grandMeister = AmpModels.grandMeisterDeluxe40
        val block = ValueBlock(mapOf(28 to 0, 29 to 120, 1 to 200), preset = 3).message()
        val state = AmpState(grandMeister).after(ProgramChange(MidiChannel.Default, 2)).after(block)
        assertEquals(
            mapOf("delay" to 0, "reverb" to 127, "modulation" to 127),
            state.values.filterKeys { it in setOf("delay", "reverb", "modulation") },
        )
    }

    @Test
    fun `An effect switch the amp reported keeps its report over its level`() {
        val grandMeister = AmpModels.grandMeisterDeluxe40
        val delayOn = ControlReport(53, 127).message()
        val block = ValueBlock(mapOf(28 to 0), preset = 3).message()
        val state = AmpState(grandMeister).after(ProgramChange(MidiChannel.Default, 2)).after(delayOn).after(block)
        assertEquals(127, state.values["delay"])
    }

    @Test
    fun `Other messages change nothing`() {
        assertEquals(start, start.after(SystemRealtime(0xF8)))
    }

    @Test
    fun `Every control of every amp reads back as the value it was set to`() {
        val wrong =
            AmpModels.all.flatMap { model ->
                model.controls.flatMap { control ->
                    listOf(0, 64, 127)
                        .filter { AmpState(model).after(control.set(it)).values[control.id] != it }
                        .map { "${model.id}/${control.id}=$it" }
                }
            }
        assertEquals(emptyList(), wrong.take(5))
    }

    @Test
    fun `An effect knob is silent only while its effect is known to be off`() {
        val time = amp.control("delay_time")!!
        val delay = amp.control("delay")!!
        assertEquals(false, start.silent(time), "switch unknown")
        assertEquals(true, start.after(delay.switched(false)).silent(time), "switch off")
        assertEquals(false, start.after(delay.switched(true)).silent(time), "switch on")
        assertEquals(false, start.after(delay.switched(false)).silent(gain), "a control that needs nothing")
    }
}
