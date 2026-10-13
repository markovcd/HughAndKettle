package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ToneTest {
    private val amp = AmpModels.blackSpirit200

    @Test
    fun `A tone recalls its values in the amp's panel order and nothing else`() {
        val tone = Tone("Lead", amp, mapOf("volume" to 90, "gain" to 100, "channel" to 79))
        val sent = tone.recall().map { it.controller }
        assertEquals(listOf(31, 56, 57), sent)
    }

    @Test
    fun `A tone recalls on the MIDI channel it is asked to`() {
        val tone = Tone("Lead", amp, mapOf("gain" to 100))
        assertEquals(listOf(ControlChange(MidiChannel(5), 56, 100)), tone.recall(MidiChannel(5)))
    }

    @Test
    fun `A tone of the amp's state holds what the state knows`() {
        val state = AmpState(amp).after(amp.control("gain")!!.set(12)).after(amp.control("mid")!!.set(3))
        assertEquals(mapOf("gain" to 12, "mid" to 3), Tone.of("Now", state).values)
    }

    @Test
    fun `A tone with a control its amp lacks is a broken invariant`() {
        assertFailsWith<IllegalArgumentException> { Tone("x", amp, mapOf("flux" to 1)) }
    }
}
