package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PresetReadingTest {
    private val amp = AmpModels.grandMeisterDeluxe40

    /** The block the amp sends after recalling [preset], gain at [gain] of 255. */
    private fun block(
        preset: Int?,
        gain: Int,
    ) = ValueBlock(mapOf(56 to gain, 21 to 100), preset).message()

    @Test
    fun `Each preset is recalled in turn and read as a tone named after it`() {
        val names = PresetNames(amp).with(2, "Lead")
        var reading = PresetReading.of(names, listOf(1, 2))
        assertEquals(ProgramChange(MidiChannel.Default, 0), reading.next)
        reading = reading.after(reading.next!!).after(block(1, 60))
        assertEquals(ProgramChange(MidiChannel.Default, 1), reading.next)
        reading = reading.after(block(2, 200))
        assertNull(reading.next)
        assertEquals(listOf("Preset 1", "2 Lead"), reading.tones.map { it.name })
        assertEquals(listOf(30 to 50, 100 to 50), reading.tones.map { it.values["gain_hard"] to it.values["bass"] })
    }

    @Test
    fun `A block for another preset or after a channel switch does not end the preset being read`() {
        val reading = PresetReading.of(PresetNames(amp), listOf(5)).after(block(4, 10)).after(block(null, 10))
        assertEquals(5, reading.preset)
        assertEquals(emptyList(), reading.tones)
    }

    @Test
    fun `A preset given up on is missed and the next one is read`() {
        val reading = PresetReading.of(PresetNames(amp), listOf(5, 6)).skip()
        assertEquals(6, reading.preset)
        assertEquals(listOf(5), reading.missed)
        assertEquals(1, reading.remaining)
    }

    @Test
    fun `Reading starts at every preset the amp has`() {
        val reading = PresetReading.of(PresetNames(amp))
        assertEquals(1, reading.preset)
        assertEquals(amp.presetCount, reading.remaining)
    }
}
