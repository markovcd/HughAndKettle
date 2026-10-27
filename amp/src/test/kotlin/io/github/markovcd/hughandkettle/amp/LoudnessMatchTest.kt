package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.amp.ChordListen.Outcome
import io.github.markovcd.hughandkettle.amp.LoudnessMatch.Stage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class LoudnessMatchTest {
    private val match = LoudnessMatch.of(listOf(7, 5, 12), reference = 5)

    private fun measured(level: Double) = Outcome.Measured(level)

    /** The match with the reference measured at -20 dB. */
    private val referenced =
        match
            .recalled(stored = 64, correction = 0, volume = 64)
            .heard(measured(-20.0))
            .first

    /**
     * Runs the preset [at][LoudnessMatch.at] against an amp whose loudness is [loudness] of the volume, from
     * [stored] with [correction]; the match after it, and the volumes sent.
     */
    private fun LoudnessMatch.played(
        stored: Int,
        correction: Int = 0,
        loudness: (Int) -> Double,
    ): Pair<LoudnessMatch, List<Int>> {
        var match = recalled(stored, correction, stored + correction)
        val sent = mutableListOf<Int>()
        val preset = match.at
        while (match.at == preset && match.stage != Stage.Done) {
            val (next, volume) = match.heard(measured(loudness(match.volume!!)))
            volume?.let(sent::add)
            match = next.adjusted()
        }
        return match to sent
    }

    @Test
    fun `The reference is measured first and the others follow in order`() {
        assertEquals(listOf(5, 7, 12), match.presets)
        assertEquals(5, match.at)
        assertEquals(Stage.Recalling, referenced.stage)
        assertEquals(7, referenced.at)
        assertEquals(-20.0, referenced.level)
    }

    @Test
    fun `A preset within a dB of the reference is matched as it is`() {
        val (after, sent) = referenced.played(72) { -20.5 }
        assertEquals(emptyList(), sent)
        assertEquals(MatchResult.Matched(7, 72, 72, 0, -0.5, true), after.results.last())
    }

    @Test
    fun `A preset too loud is turned down until it matches, by the slope it shows`() {
        // Half a dB a step, louder than the first guess: the second step corrects for it.
        val (after, sent) = referenced.played(60) { volume -> -20.0 + 0.5 * (volume - 50) }
        assertEquals(50, sent.last())
        val result = assertIs<MatchResult.Matched>(after.results.last())
        assertEquals(-10, result.correction)
        assertEquals(true, result.reached)
    }

    @Test
    fun `The first step is a guess at a third of a dB a step`() {
        val (_, volume) = referenced.recalled(60, 0, 60).heard(measured(-16.0))
        assertEquals(49, volume)
        assertEquals(
            Stage.Adjusting(1, 4.0, 60, 49),
            referenced
                .recalled(60, 0, 60)
                .heard(measured(-16.0))
                .first.stage,
        )
    }

    @Test
    fun `A preset that cannot get loud enough stops at the top of the knob`() {
        val (after, sent) = referenced.played(118) { volume -> -22.5 - 0.35 * (127 - volume) }
        assertEquals(127, sent.last())
        assertEquals(MatchResult.Matched(7, 118, 127, 9, -2.5, false), after.results.last())
    }

    @Test
    fun `A correction already kept is carried into the new one`() {
        val (after, _) = referenced.played(stored = 60, correction = -5) { volume -> -20.0 + 0.35 * (volume - 50) }
        assertEquals(-10, assertIs<MatchResult.Matched>(after.results.last()).correction)
    }

    @Test
    fun `After five tries the closest volume is kept`() {
        // Loudness that jumps about whatever the volume: never within a dB.
        var flip = 1.0
        val (after, sent) =
            referenced.played(60) {
                flip = -flip
                -20.0 + 3 * flip
            }
        assertEquals(5, sent.size)
        assertEquals(3.0, kotlin.math.abs(assertIs<MatchResult.Matched>(after.results.last()).off), 0.001)
    }

    @Test
    fun `A chord that cannot be measured is listened for again on the next try`() {
        val retried = referenced.recalled(60, 0, 60).heard(Outcome.Retry(ChordListen.Why.TooQuiet)).first
        assertEquals(Stage.Retry(2, ChordListen.Why.TooQuiet), retried.stage)
        assertEquals(Stage.Adjusting(2, 4.0, 60, 49), retried.heard(measured(-16.0)).first.stage)
    }

    @Test
    fun `A skipped preset keeps its correction and the match moves on`() {
        val skipped = referenced.recalled(55, 0, 55).skip()
        assertEquals(MatchResult.Skipped(7, 55), skipped.results.last())
        assertEquals(12, skipped.at)
        assertEquals(mapOf(5 to 0), skipped.corrections())
    }

    @Test
    fun `Skipping the reference stops the match with nothing matched`() {
        val stopped = match.skip()
        assertEquals(Stage.Done, stopped.stage)
        assertEquals(emptyMap(), stopped.corrections())
        assertEquals(3, stopped.results.size)
    }

    @Test
    fun `Stopping skips every preset left`() {
        val stopped = referenced.stop()
        assertEquals(listOf(5, 7, 12), stopped.results.map { it.preset })
        assertEquals(null, stopped.at)
    }

    @Test
    fun `The corrections are what each preset ends at against what it stored`() {
        val (seven, _) = referenced.played(60) { volume -> -20.0 + 0.35 * (volume - 49) }
        val (done, _) = seven.played(40) { volume -> -20.0 + 0.35 * (volume - 40) }
        assertEquals(Stage.Done, done.stage)
        assertEquals(mapOf(5 to 0, 7 to -11, 12 to 0), done.corrections())
    }
}
