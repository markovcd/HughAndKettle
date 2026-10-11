package io.github.markovcd.hughandkettle.amp

/** The one place that knows how 0..127 is cut into equal sectors. */
public object Sectors {
    /** The middle value of sector [index] of [count], so a small drift on either side still lands in it. */
    public fun value(
        index: Int,
        count: Int,
    ): Int {
        require(index in 0 until count) { "sector $index is outside 0 until $count" }
        val lowest = first(index, count)
        val highest = first(index + 1, count) - 1
        return (lowest + highest) / 2
    }

    /** The sector a value of 0..127 falls in. */
    public fun index(
        value: Int,
        count: Int,
    ): Int {
        require(value in 0..127) { "value must be 0..127, got $value" }
        return value * count / 128
    }

    /** The lowest value in sector [index]: the smallest v with v * count / 128 == index. */
    private fun first(
        index: Int,
        count: Int,
    ): Int = (index * 128 + count - 1) / count
}
