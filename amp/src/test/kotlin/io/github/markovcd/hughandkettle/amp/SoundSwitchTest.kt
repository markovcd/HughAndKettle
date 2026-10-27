package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SoundSwitchTest {
    private val amp = AmpModels.grandMeisterDeluxe40

    @Test
    fun `The amp's value block after a recall reports a switch to that preset`() {
        val block = ValueBlock(mapOf(4 to 200), preset = 5).message()
        assertEquals(SoundSwitch(5, reported = true), SoundSwitch.heard(block, amp))
    }

    @Test
    fun `A heard program change reports a switch to its preset, counted from 1`() {
        assertEquals(SoundSwitch(3, reported = true), SoundSwitch.heard(ProgramChange(MidiChannel(1), 2), amp))
    }

    @Test
    fun `A channel switch and a knob report no switch`() {
        assertNull(SoundSwitch.heard(ValueBlock(mapOf(4 to 200), preset = null).message(), amp))
        assertNull(SoundSwitch.heard(ControlChange(MidiChannel(1), 4, 60), amp))
    }
}
