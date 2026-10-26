package io.github.markovcd.hughandkettle.amp

import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Taps on a beat, as the times they came at. The beat is the slope of the straight line through the last
 * dozen taps, so a shaky tap sways it less than it would an average of the gaps. A gap more than a quarter
 * off the beat so far starts a new tempo from the tap before it, and a pause longer than [LONGEST] starts
 * afresh, since no delay is that long.
 */
public data class TapTempo(
    val taps: List<Duration> = emptyList(),
) {
    /** The taps after one more at [at], measured from any fixed point the caller keeps. */
    public fun after(at: Duration): TapTempo {
        val last = taps.lastOrNull()
        if (last == null || at - last > LONGEST || at < last) return TapTempo(listOf(at))
        val gap = at - last
        val beat = beat
        if (beat != null &&
            (gap < beat * (1 - OFF_BEAT) || gap > beat * (1 + OFF_BEAT))
        ) {
            return TapTempo(listOf(last, at))
        }
        return TapTempo((taps + at).takeLast(KEPT))
    }

    /** The beat, once there are two taps. */
    val beat: Duration?
        get() {
            if (taps.size < 2) return null
            val middle = (taps.size - 1) / 2.0
            val mean = taps.sumOf { it.inWholeNanoseconds.toDouble() } / taps.size
            var across = 0.0
            var spread = 0.0
            taps.forEachIndexed { index, at ->
                across += (index - middle) * (at.inWholeNanoseconds - mean)
                spread += (index - middle) * (index - middle)
            }
            return (across / spread).nanoseconds
        }

    public companion object {
        public val LONGEST: Duration = 2.seconds

        // A dozen taps evens out a shaky hand; a tempo change is followed by OFF_BEAT, not by forgetting.
        private const val KEPT = 12
        private const val OFF_BEAT = 0.25
    }
}
