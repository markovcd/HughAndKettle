package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cable_failed
import io.github.markovcd.hughandkettle.ui.cable_open
import io.github.markovcd.hughandkettle.ui.cable_opening
import io.github.markovcd.hughandkettle.ui.connected_to
import io.github.markovcd.hughandkettle.ui.connecting_again_to
import io.github.markovcd.hughandkettle.ui.connecting_to
import io.github.markovcd.hughandkettle.ui.link_lost
import io.github.markovcd.hughandkettle.ui.link_needs_address
import io.github.markovcd.hughandkettle.ui.link_refused
import io.github.markovcd.hughandkettle.ui.link_unreachable
import io.github.markovcd.hughandkettle.ui.not_connected
import io.github.markovcd.hughandkettle.ui.send_failed
import io.github.markovcd.hughandkettle.wmi.LinkFailure
import io.github.markovcd.hughandkettle.wmi.LinkState

/** The one place a link's or a cable's state becomes words, for the panel and the notification alike. */
object LinkWords {
    /** The cable's words while there is a cable, else the link's. */
    fun of(
        state: LinkState,
        cable: CableState,
    ): Words =
        when (cable) {
            CableState.None -> of(state)
            is CableState.Opening -> Words(Res.string.cable_opening, listOf(cable.cable.name))
            is CableState.Open -> Words(Res.string.cable_open, listOf(cable.cable.name))
            is CableState.Failed -> Words(Res.string.cable_failed, listOf(cable.cable.name, cable.reason))
        }

    private fun of(state: LinkState): Words =
        when (state) {
            LinkState.Idle -> {
                Words(Res.string.not_connected)
            }

            is LinkState.Connecting -> {
                if (state.attempt == 1) {
                    Words(Res.string.connecting_to, listOf("${state.address}"))
                } else {
                    Words(Res.string.connecting_again_to, listOf("${state.address}", state.attempt))
                }
            }

            is LinkState.Connected -> {
                Words(Res.string.connected_to, listOf("${state.address}"))
            }

            is LinkState.Retrying -> {
                when (val failure = state.failure) {
                    LinkFailure.Refused -> {
                        Words(Res.string.link_refused, listOf("${state.address}"))
                    }

                    is LinkFailure.Unreachable -> {
                        Words(Res.string.link_unreachable, listOf("${state.address}", failure.reason))
                    }

                    is LinkFailure.Lost -> {
                        Words(Res.string.link_lost, listOf("${state.address}", failure.reason))
                    }
                }
            }
        }

    fun of(failure: PanelFailure): Words =
        when (failure) {
            PanelFailure.NeedsAddress -> Words(Res.string.link_needs_address)
            is PanelFailure.SendFailed -> Words(Res.string.send_failed, listOf(failure.reason))
        }
}
