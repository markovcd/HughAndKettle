package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RecallAnswersTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val volume = amp.control("volume")!!
    private val mute = amp.control("mute")!!
    private val time = amp.control("delay_time")!!
    private val both = RecallAnswers(TempoLock(time, 40), VolumeTrim.of(volume, -20))

    /** The value block a recall of [preset] sends, its knobs of 0..127 reported in 256 steps. */
    private fun recalled(preset: Int) =
        ValueBlock(mapOf(volume.controller to 2 * 60, time.controller to 2 * 90), preset, 0).message()

    @Test
    fun `A recalled preset under a lock and a trim gets its volume trimmed and the locked time back`() {
        val answered = both.heard(recalled(3))
        assertEquals(listOf(volume.set(40)), answered.trimmed)
        assertEquals(time.set(40), answered.restored)
        assertEquals(listOf(volume.set(40), time.set(40)), answered.replies)
    }

    @Test
    fun `A recall sent under a trim goes behind the mute and the value block lifts it`() {
        val (behind, messages) = both.recall(amp.recall(3), mute, muted = false, then = listOf(time.set(40)))
        assertEquals(listOf(mute.switched(true), amp.recall(3), time.set(40)), messages)
        assertTrue(behind.muting)
        val answered = behind.heard(recalled(3))
        assertEquals(listOf(volume.set(40), mute.switched(false)), answered.trimmed)
        assertFalse(answered.answers.muting)
    }

    @Test
    fun `A recall sent with no trim, or with the amp muted already, goes out as it is`() {
        val plain = RecallAnswers(lock = TempoLock(time, 40))
        assertEquals(listOf(amp.recall(3)), plain.recall(amp.recall(3), mute, muted = false).second)
        assertEquals(listOf(amp.recall(3)), both.recall(amp.recall(3), mute, muted = true).second)
    }

    @Test
    fun `A recall whose value block never comes is lifted from behind the mute`() {
        val (behind, _) = both.recall(amp.recall(3), mute, muted = false)
        val (lifted, lift) = behind.unmuted()
        assertEquals(mute.switched(false), lift)
        assertFalse(lifted.muting)
        assertEquals(null, lifted.unmuted().second)
    }

    @Test
    fun `A tone recalled keeps the locked time and plays at the trim`() {
        assertEquals(listOf(time.set(40), volume.set(40)), both.tone(listOf(time.set(100), volume.set(60))))
    }
}
