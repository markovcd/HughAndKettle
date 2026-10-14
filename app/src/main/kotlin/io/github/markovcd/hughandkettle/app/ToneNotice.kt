package io.github.markovcd.hughandkettle.app

/**
 * What the tone shelf has to tell after a change: a failure, what an import of a tone or a preset file
 * brought in, or what a reading of the presets did.
 */
sealed interface ToneNotice {
    data class Failed(
        val reason: String,
    ) : ToneNotice

    data class Imported(
        val name: String,
        val skipped: List<String>,
    ) : ToneNotice

    data class ImportedPresets(
        val count: Int,
        val skipped: List<String>,
    ) : ToneNotice

    data class Read(
        val count: Int,
        val missed: List<Int>,
    ) : ToneNotice
}
