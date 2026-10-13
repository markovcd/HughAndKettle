package io.github.markovcd.hughandkettle.wmi

import java.net.ConnectException
import java.net.SocketTimeoutException

/** Why there is no link, as a value a screen or `hk` turns into words. */
public sealed interface LinkFailure {
    /** The box answered and said no: the WMI-1 serves one client, and something else may hold it. */
    public data object Refused : LinkFailure

    public data class Unreachable(
        val reason: String,
    ) : LinkFailure

    /** A link that was open went away. */
    public data class Lost(
        val reason: String,
    ) : LinkFailure

    public companion object {
        /** What a failed [WmiLink.open] means. */
        public fun of(failure: Throwable): LinkFailure =
            when (failure) {
                is ConnectException -> Refused
                is SocketTimeoutException -> Unreachable("no answer within ${WmiDefaults.ConnectTimeout}")
                else -> Unreachable(failure.message ?: failure.javaClass.simpleName)
            }
    }
}
