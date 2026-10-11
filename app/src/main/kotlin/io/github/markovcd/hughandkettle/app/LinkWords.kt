package io.github.markovcd.hughandkettle.app

import android.content.res.Resources
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.wmi.LinkFailure
import io.github.markovcd.hughandkettle.wmi.LinkState

/** The one place a link's state becomes words, for the panel and the notification alike. */
internal object LinkWords {
    fun of(
        state: LinkState,
        resources: Resources,
    ): String =
        when (state) {
            LinkState.Idle -> {
                resources.getString(R.string.not_connected)
            }

            is LinkState.Connecting -> {
                if (state.attempt == 1) {
                    resources.getString(R.string.connecting_to, "${state.address}")
                } else {
                    resources.getString(R.string.connecting_again_to, "${state.address}", state.attempt)
                }
            }

            is LinkState.Connected -> {
                resources.getString(R.string.connected_to, "${state.address}")
            }

            is LinkState.Retrying -> {
                when (val failure = state.failure) {
                    LinkFailure.Refused -> {
                        resources.getString(R.string.link_refused, "${state.address}")
                    }

                    is LinkFailure.Unreachable -> {
                        resources.getString(R.string.link_unreachable, "${state.address}", failure.reason)
                    }

                    is LinkFailure.Lost -> {
                        resources.getString(R.string.link_lost, "${state.address}", failure.reason)
                    }
                }
            }
        }

    fun of(
        failure: PanelFailure,
        resources: Resources,
    ): String =
        when (failure) {
            PanelFailure.NeedsAddress -> resources.getString(R.string.link_needs_address)
            is PanelFailure.SendFailed -> resources.getString(R.string.send_failed, failure.reason)
        }
}
