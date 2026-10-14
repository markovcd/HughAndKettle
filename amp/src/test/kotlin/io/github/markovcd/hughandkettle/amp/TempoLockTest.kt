package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TempoLockTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val time = amp.control("delay_time")!!
    private val gain = amp.control("gain")!!
    private val held = TempoLock(time, 40)

    /** The value block a preset with [delay] of 0..127 sends, as the amp reports it in 256 steps. */
    private fun recalled(delay: Int) = ValueBlock(mapOf(4 to 2 * delay + 1, 56 to 90), preset = 3).message()

    @Test
    fun `A recalled preset with another delay time is answered with the held one`() {
        assertEquals(time.set(40), held.restore(recalled(70)))
    }

    @Test
    fun `A recalled preset with the held delay time needs nothing sent`() {
        assertNull(held.restore(recalled(40)))
    }

    @Test
    fun `A recall does not move what is held`() {
        assertEquals(held, held.after(recalled(70)))
    }

    @Test
    fun `Setting the time by hand, by a tap or from the amp moves what is held`() {
        assertEquals(55, held.after(time.set(55)).value)
        assertEquals(55, held.after(ControlReport(4, 111).message()).value)
    }

    @Test
    fun `Another control moves nothing`() {
        assertEquals(held, held.after(gain.set(3)).after(ControlReport(56, 9).message()))
    }

    @Test
    fun `A lock with nothing held takes the first time heard, and restores nothing until then`() {
        val empty = TempoLock(time)
        assertNull(empty.restore(recalled(70)))
        assertEquals(70, empty.after(recalled(70)).value)
    }

    @Test
    fun `A tone recalled while locked sends the held time in place of its own`() {
        val tone = listOf(gain.set(9), ControlChange(MidiChannel(2), 4, 100))
        assertEquals(listOf(gain.set(9), ControlChange(MidiChannel(2), 4, 40)), held.kept(tone))
    }
}
