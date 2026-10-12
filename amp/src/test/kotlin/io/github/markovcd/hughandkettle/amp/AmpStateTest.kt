package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
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
}
