package io.github.markovcd.hughandkettle.amp

/**
 * The loudness match's corrections of an amp model's presets: an amount added to a preset's stored volume
 * each time it is recalled, by preset counted from 1. A preset without one plays its own volume.
 */
public data class LoudnessCorrections(
    val amp: AmpModel,
    val amounts: Map<Int, Int> = emptyMap(),
) {
    init {
        val outside = amounts.keys.filter { it !in 1..amp.presetCount }
        require(outside.isEmpty()) { "${amp.name} has presets 1..${amp.presetCount}, not $outside" }
        val wide = amounts.filterValues { it !in -127..127 || it == 0 }
        require(wide.isEmpty()) { "a correction is -127..127 and not 0, not $wide" }
    }

    public operator fun get(preset: Int): Int? = amounts[preset]

    /** These corrections with [amounts] in place of the ones for the same presets, an amount of 0 taking one away. */
    public fun with(amounts: Map<Int, Int>): LoudnessCorrections =
        copy(amounts = (this.amounts + amounts).filterValues { it != 0 })

    /** These corrections without [preset]'s. */
    public fun without(preset: Int): LoudnessCorrections = copy(amounts = amounts - preset)
}
