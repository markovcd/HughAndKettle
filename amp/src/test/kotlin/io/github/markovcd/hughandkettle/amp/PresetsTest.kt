package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PresetsTest {
    @Test
    fun `Preset 1 is program 0 and preset 128 is program 127`() {
        assertEquals(ProgramChange(MidiChannel.Default, 0), AmpModels.blackSpirit200.recall(1))
        assertEquals(ProgramChange(MidiChannel.Default, 127), AmpModels.blackSpirit200.recall(128))
    }

    @Test
    fun `A preset outside the amp's range is refused`() {
        assertFailsWith<IllegalArgumentException> { AmpModels.blackSpirit200.recall(0) }
        assertFailsWith<IllegalArgumentException> { AmpModels.blackSpirit200.recall(129) }
    }

    @Test
    fun `The GrandMeister shows its presets as 32 banks of 4`() {
        val amp = AmpModels.grandMeisterDeluxe40
        assertEquals(PresetSlot(1, 1), amp.slotOf(1))
        assertEquals(PresetSlot(1, 4), amp.slotOf(4))
        assertEquals(PresetSlot(2, 1), amp.slotOf(5))
        assertEquals(PresetSlot(32, 4), amp.slotOf(128))
    }

    @Test
    fun `An amp whose manual names no banks numbers presets straight through`() {
        assertNull(AmpModels.blackSpirit200.slotOf(17))
    }
}
