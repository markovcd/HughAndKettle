package io.github.markovcd.hughandkettle.amp

/**
 * The names given to an amp model's presets, by preset counted from 1. The amp names none of its
 * presets; these live on the phone or with `hk`, and a preset without one goes by its number.
 */
public data class PresetNames(
    val amp: AmpModel,
    val names: Map<Int, String> = emptyMap(),
) {
    init {
        val outside = names.keys.filter { it !in 1..amp.presetCount }
        require(outside.isEmpty()) { "${amp.name} has presets 1..${amp.presetCount}, not $outside" }
        require(names.values.none { it.isBlank() }) { "a preset name cannot be blank" }
    }

    public operator fun get(preset: Int): String? = names[preset]

    /** These names with [preset] called [name], or unnamed when [name] is blank. */
    public fun with(
        preset: Int,
        name: String,
    ): PresetNames {
        require(preset in 1..amp.presetCount) { "${amp.name} has presets 1..${amp.presetCount}, not $preset" }
        val trimmed = name.trim()
        return copy(names = if (trimmed.isEmpty()) names - preset else names + (preset to trimmed))
    }

    /** What a tone read from [preset] is called: its number, then its name when it has one. */
    public fun toneName(preset: Int): String = names[preset]?.let { "$preset $it" } ?: "Preset $preset"
}
