package io.github.markovcd.hughandkettle.wmi

import java.net.ConnectException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Whether a link to a WMI-1 is wanted, and how getting one is going.
 *
 * Each transition returns the next state; one that does not apply to the current state returns it
 * unchanged, so a late report from an attempt that was cancelled changes nothing. Whoever opens links
 * feeds in what happened and does what the state says: open a link while [Connecting], and while
 * [Retrying] wait [Retrying.wait], then [retry]. A wanted link is retried until it is cancelled.
 */
public sealed interface LinkState {
    public data object Idle : LinkState

    /** An attempt to open a link to [target] is under way, after [failures] failed attempts in a row. */
    public data class Connecting(
        val target: Target,
        val failures: Int,
    ) : LinkState

    public data class Connected(
        val target: Target,
    ) : LinkState

    /** There is no link to [target] because of [failure]; the next attempt is due after [wait]. */
    public data class Retrying(
        val target: Target,
        val failures: Int,
        val failure: Failure,
        val wait: Duration,
    ) : LinkState

    /** Why there is no link right now. */
    public sealed interface Failure {
        /** The box answered and said no: it serves one client, and something else, such as the iPad app, may hold it. */
        public data object Refused : Failure

        public data class Unreachable(
            val reason: String,
        ) : Failure

        public data class Lost(
            val reason: String,
        ) : Failure

        public companion object {
            /** What a failure to open a link means. */
            public fun of(failure: Throwable): Failure =
                if (failure is ConnectException) {
                    Refused
                } else {
                    Unreachable(failure.message ?: failure.javaClass.simpleName)
                }
        }
    }

    public fun connect(target: Target): LinkState = if (this is Idle) Connecting(target, failures = 0) else this

    public fun opened(): LinkState = if (this is Connecting) Connected(target) else this

    public fun failed(failure: Failure): LinkState =
        if (this is Connecting) Retrying(target, failures + 1, failure, backoff(failures + 1)) else this

    /** A link that was up is tried again at once: the box may have dropped it for a moment. */
    public fun lost(reason: String): LinkState =
        if (this is Connected) Retrying(target, failures = 0, Failure.Lost(reason), Duration.ZERO) else this

    public fun retry(): LinkState = if (this is Retrying) Connecting(target, failures) else this

    public fun cancel(): LinkState = Idle

    private companion object {
        val FirstWait = 1.seconds
        val LongestWait = 10.seconds

        /** Doubles from [FirstWait] with each failure in a row, up to [LongestWait]. */
        fun backoff(failures: Int): Duration = (FirstWait * (1 shl minOf(failures - 1, 4))).coerceAtMost(LongestWait)
    }
}
