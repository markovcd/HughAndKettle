package io.github.markovcd.hughandkettle.wmi

import kotlin.time.Duration

/**
 * Where the link to one WMI-1 stands. A failed or lost link is retried with [Backoff] until stopped,
 * so the only way back to [Idle] is [LinkEvent.Stop]. Pure: time is passed in as the time since an
 * origin the caller chooses, and nothing here touches the network.
 */
public sealed interface LinkState {
    public data object Idle : LinkState

    /** Opening; [attempt] counts from 1 since the link was last up or asked for. */
    public data class Connecting(
        val address: WmiAddress,
        val attempt: Int,
    ) : LinkState

    public data class Connected(
        val address: WmiAddress,
    ) : LinkState

    /** Waiting until [at] to try again, after [failures] failures in a row, the last for [failure]. */
    public data class Retrying(
        val address: WmiAddress,
        val failures: Int,
        val failure: LinkFailure,
        val at: Duration,
    ) : LinkState

    /** The state after [event] at [now]; an event that means nothing here leaves the state as it is. */
    public fun on(
        event: LinkEvent,
        now: Duration,
        backoff: Backoff,
    ): LinkState =
        when (event) {
            is LinkEvent.Connect -> {
                Connecting(event.address, 1)
            }

            LinkEvent.Stop -> {
                Idle
            }

            LinkEvent.Opened -> {
                if (this is Connecting) Connected(address) else this
            }

            is LinkEvent.Failed -> {
                when (this) {
                    is Connecting -> Retrying(address, attempt, event.failure, now + backoff.after(attempt))
                    is Connected -> Retrying(address, 1, event.failure, now + backoff.after(1))
                    else -> this
                }
            }

            LinkEvent.Tick -> {
                if (this is Retrying && now >= at) Connecting(address, failures + 1) else this
            }
        }
}
