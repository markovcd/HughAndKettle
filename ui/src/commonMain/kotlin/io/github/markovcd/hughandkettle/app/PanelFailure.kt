package io.github.markovcd.hughandkettle.app

/** What went wrong on the panel itself, beside the link's own state. */
sealed interface PanelFailure {
    data object NeedsAddress : PanelFailure

    data class SendFailed(
        val reason: String,
    ) : PanelFailure
}
