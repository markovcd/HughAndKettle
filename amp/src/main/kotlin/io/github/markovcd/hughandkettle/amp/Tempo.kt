package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToInt

/** How a delay time reads as a tempo: one beat a repeat. */
public object Tempo {
    /** The beats per minute a repeat every [ms] milliseconds makes, to the nearest whole beat. */
    public fun beatsPerMinute(ms: Int): Int {
        require(ms > 0) { "a delay time is longer than 0 ms, got $ms" }
        return (60_000.0 / ms).roundToInt()
    }
}
