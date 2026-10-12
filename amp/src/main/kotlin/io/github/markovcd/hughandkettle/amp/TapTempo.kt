package io.github.markovcd.hughandkettle.amp

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Taps on a beat, as the times they came at. The beat is the mean gap of the last few taps; a pause
 * longer than [LONGEST] starts afresh, since no delay is that long.
 */
public data class TapTempo(
    val taps: List<Duration> = emptyList(),
) {
    /** The taps after one more at [at], measured from any fixed point the caller keeps. */
    public fun after(at: Duration): TapTempo {
        val last = taps.lastOrNull()
        if (last == null || at - last > LONGEST || at < last) return TapTempo(listOf(at))
        return TapTempo((taps + at).takeLast(KEPT))
    }

    /** The beat, once there are two taps. */
    val beat: Duration? get() = if (taps.size < 2) null else (taps.last() - taps.first()) / (taps.size - 1)

    public companion object {
        public val LONGEST: Duration = 2.seconds

        // Four taps, three gaps: enough to even out a shaky hand, few enough to follow a tempo change.
        private const val KEPT = 4
    }
}
