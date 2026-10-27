package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ChordListenTest {
    /** How a listen hearing [levels] in turn ends, each frame peaking at [peak]. */
    private fun heard(
        levels: List<Double>,
        peak: Double = 0.5,
    ): ChordListen.Outcome? =
        levels.fold(ChordListen.start()) { listen, level -> listen.after(LevelFrame(level, peak)) }.outcome

    private fun room(
        frames: Int,
        level: Double = -70.0,
    ) = List(frames) { level }

    private fun strum(
        level: Double,
        frames: Int,
    ) = List(5) { level + 6 } + List(frames) { level }

    @Test
    fun `A chord held after a quiet room rings at the mean of its ring`() {
        val outcome = heard(room(10) + strum(-20.0, 30))
        assertEquals(-20.0, assertIs<ChordListen.Outcome.Measured>(outcome).level, 0.001)
    }

    @Test
    fun `The attack of the strum is left out`() {
        val outcome = heard(room(10) + List(5) { -5.0 } + List(30) { -30.0 })
        assertEquals(-30.0, assertIs<ChordListen.Outcome.Measured>(outcome).level, 0.001)
    }

    @Test
    fun `A listen goes on while the chord still rings`() {
        assertNull(heard(room(10) + strum(-20.0, 29)))
    }

    @Test
    fun `A strum before the room is heard is not taken for one`() {
        assertNull(heard(room(5) + strum(-20.0, 30)))
    }

    @Test
    fun `A chord that dies early is measured on what rang, if it rang long enough`() {
        assertIs<ChordListen.Outcome.Measured>(heard(room(10) + strum(-20.0, 12) + room(1)))
        assertEquals(ChordListen.Outcome.Retry(ChordListen.Why.TooQuiet), heard(room(10) + strum(-20.0, 5) + room(1)))
    }

    @Test
    fun `No strum in ten seconds is too quiet, or too noisy in a loud room`() {
        assertEquals(ChordListen.Outcome.Retry(ChordListen.Why.TooQuiet), heard(room(200)))
        assertEquals(ChordListen.Outcome.Retry(ChordListen.Why.TooNoisy), heard(room(200, -30.0)))
    }

    @Test
    fun `A strum too soft for the microphone is not one`() {
        assertNull(heard(room(10, -90.0) + strum(-70.0, 30)))
    }

    @Test
    fun `A chord that clips the microphone is too loud`() {
        assertEquals(ChordListen.Outcome.Retry(ChordListen.Why.TooLoud), heard(room(10) + strum(-1.0, 30), peak = 1.0))
    }

    @Test
    fun `A chord barely above the room is too noisy`() {
        assertEquals(ChordListen.Outcome.Retry(ChordListen.Why.TooNoisy), heard(room(10, -50.0) + strum(-38.0, 30)))
    }
}
