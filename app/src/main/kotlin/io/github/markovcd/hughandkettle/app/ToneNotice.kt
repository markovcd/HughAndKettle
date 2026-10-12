package io.github.markovcd.hughandkettle.app

/** What the tone shelf has to tell after a change: a failure, or what an import brought in. */
sealed interface ToneNotice {
    data class Failed(
        val reason: String,
    ) : ToneNotice

    data class Imported(
        val name: String,
        val skipped: List<String>,
    ) : ToneNotice
}
