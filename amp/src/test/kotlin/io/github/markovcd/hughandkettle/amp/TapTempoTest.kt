package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class TapTempoTest {
    private fun tapped(vararg ms: Int): TapTempo = ms.fold(TapTempo()) { taps, at -> taps.after(at.milliseconds) }

    @Test
    fun `One tap has no beat yet`() {
        assertNull(tapped(1000).beat)
    }

    @Test
    fun `The beat is the slope of the straight line through the taps`() {
        assertEquals(502.milliseconds, tapped(0, 480, 1000, 1500).beat)
    }

    @Test
    fun `A dozen shaky taps at 100 BPM read as 100 BPM`() {
        val shake = listOf(0, 25, -25, 20, -20, 25, -25, 15, -15, 25, -25, 0)
        val taps = tapped(*shake.mapIndexed { beat, off -> beat * 600 + off }.toIntArray())
        assertEquals(100, (1.minutes / taps.beat!!).roundToInt())
    }

    @Test
    fun `A tap far off the beat starts a new tempo from the tap before it`() {
        assertEquals(300.milliseconds, tapped(0, 1000, 2000, 2300, 2600, 2900).beat)
    }

    @Test
    fun `A pause longer than any delay starts afresh`() {
        val taps = tapped(0, 500, 1000, 3500)
        assertEquals(listOf<Duration>(3500.milliseconds), taps.taps)
        assertEquals(400.milliseconds, taps.after(3900.milliseconds).beat)
    }

    @Test
    fun `A tap earlier than the last one starts afresh`() {
        assertEquals(listOf<Duration>(100.milliseconds), tapped(500, 100).taps)
    }
}
