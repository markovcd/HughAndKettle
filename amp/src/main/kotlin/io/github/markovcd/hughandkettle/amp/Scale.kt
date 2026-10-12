package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToInt

/**
 * What a knob's 0..127 means on the amp, where the manual says: [lowest] at 0 to [highest] at 127, in
 * [unit]. The manual gives the ends only; the steps between are taken as even until the hardware says
 * otherwise (TODO.md).
 */
public data class Scale(
    val lowest: Int,
    val highest: Int,
    val unit: String,
) {
    init {
        require(lowest < highest) { "a scale runs up, got $lowest..$highest" }
    }

    /** The amount a knob at [value] stands for, to the nearest whole [unit]. */
    public fun amount(value: Int): Int {
        require(value in 0..127) { "value must be 0..127, got $value" }
        return lowest + ((highest - lowest) * value / 127.0).roundToInt()
    }

    /** The knob value nearest to [amount]; past either end, that end. */
    public fun value(amount: Int): Int =
        ((amount.coerceIn(lowest, highest) - lowest) * 127.0 / (highest - lowest)).roundToInt()
}
