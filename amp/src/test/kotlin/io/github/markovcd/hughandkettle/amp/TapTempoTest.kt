package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class TapTempoTest {
    private fun tapped(vararg ms: Int): TapTempo = ms.fold(TapTempo()) { taps, at -> taps.after(at.milliseconds) }

    @Test
    fun `One tap has no beat yet`() {
        assertNull(tapped(1000).beat)
    }

    @Test
    fun `The beat is the mean gap between the taps`() {
        assertEquals(500.milliseconds, tapped(0, 480, 1000, 1500).beat)
    }

    @Test
    fun `Only the last four taps count, so a tempo change is followed`() {
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
