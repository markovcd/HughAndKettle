package io.github.markovcd.hughandkettle.app

/** What the tone shelf has to tell after a change: a failure, what an import brought in, or what a reading of the presets did. */
sealed interface ToneNotice {
    data class Failed(
        val reason: String,
    ) : ToneNotice

    data class Imported(
        val name: String,
        val skipped: List<String>,
    ) : ToneNotice

    data class Read(
        val count: Int,
        val missed: List<Int>,
    ) : ToneNotice
}
