package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Sectors

/** How an effect card words its controls and their values. */
internal object CardWords {
    /** [control]'s name on [switch]'s own card, where the effect's name need not lead it: "Delay time" reads "Time". */
    fun short(
        switch: Control,
        control: Control,
    ): String = control.name.removePrefix("${switch.name} ").replaceFirstChar(Char::uppercase)

    /** [value] as the amp means it: "505 ms" on a scale, "Phaser 70" on a selector, else the number. */
    fun amount(
        control: Control,
        value: Int,
    ): String {
        val scale = control.scale
        val kind = control.kind
        return when {
            scale != null -> "${scale.amount(value)} ${scale.unit}"
            kind is ControlKind.Selector -> "${option(kind, value)} $value"
            else -> "$value"
        }
    }

    /** The option of [selector] that [value] picks. */
    fun option(
        selector: ControlKind.Selector,
        value: Int,
    ): String = selector.options[Sectors.index(value, selector.options.size)]

    /** A rate in Hz without a needless fraction: "6", "0.25". */
    fun hz(hz: Double): String = if (hz % 1.0 == 0.0) "${hz.toInt()}" else "$hz"
}
