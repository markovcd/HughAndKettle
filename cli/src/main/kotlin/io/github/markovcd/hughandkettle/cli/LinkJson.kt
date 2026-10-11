package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.LinkFailure
import io.github.markovcd.hughandkettle.wmi.LinkState

/** How a link's state is spelled on the CLI's output. */
public object LinkJson {
    public fun fields(state: LinkState): List<Pair<String, Any?>> =
        when (state) {
            LinkState.Idle -> {
                listOf("link" to "idle")
            }

            is LinkState.Connecting -> {
                listOf("link" to "connecting", "address" to "${state.address}", "attempt" to state.attempt)
            }

            is LinkState.Connected -> {
                listOf("link" to "connected", "address" to "${state.address}")
            }

            is LinkState.Retrying -> {
                listOf("link" to "retrying", "address" to "${state.address}", "failures" to state.failures) +
                    failure(state.failure)
            }
        }

    private fun failure(failure: LinkFailure): List<Pair<String, Any?>> =
        when (failure) {
            LinkFailure.Refused -> listOf("failure" to "refused")
            is LinkFailure.Unreachable -> listOf("failure" to "unreachable", "reason" to failure.reason)
            is LinkFailure.Lost -> listOf("failure" to "lost", "reason" to failure.reason)
        }
}
