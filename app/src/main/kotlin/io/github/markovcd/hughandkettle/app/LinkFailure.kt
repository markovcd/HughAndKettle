package io.github.markovcd.hughandkettle.app

/** Why the panel has no link, as a value the screen turns into words. */
sealed interface LinkFailure {
    data object NeedsAddress : LinkFailure

    /** The box answered and said no: the WMI-1 serves one client, and something else holds it. */
    data class Refused(
        val target: String,
    ) : LinkFailure

    data class Unreachable(
        val target: String,
        val reason: String,
    ) : LinkFailure

    data class SendFailed(
        val reason: String,
    ) : LinkFailure
}
