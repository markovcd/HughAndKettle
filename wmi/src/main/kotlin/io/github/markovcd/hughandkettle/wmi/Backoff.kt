package io.github.markovcd.hughandkettle.wmi

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** How long to wait before retrying a link: [first] after one failure, doubling to [ceiling]. */
public data class Backoff(
    val first: Duration,
    val ceiling: Duration,
) {
    init {
        require(
            first.isPositive() && ceiling >= first,
        ) { "a backoff needs 0 < first <= ceiling, got $first and $ceiling" }
    }

    /** The wait after [failures] failures in a row, one or more. */
    public fun after(failures: Int): Duration {
        require(failures >= 1) { "a backoff starts at one failure, got $failures" }
        var wait = first
        repeat(failures - 1) {
            wait *= 2
            if (wait >= ceiling) return ceiling
        }
        return wait
    }

    public companion object {
        /** Quick enough to catch a WMI-1 back from a power cycle, slow enough not to hammer one held by the iPad. */
        public val Default: Backoff = Backoff(1.seconds, 30.seconds)
    }
}
