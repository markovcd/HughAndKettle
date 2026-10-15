package io.github.markovcd.hughandkettle.amp

/** One step of a [Song]: the amp's [preset], counted from 1, held for [beats]. */
public data class SongStep(
    val preset: Int,
    val beats: Int,
) {
    init {
        require(beats in 1..MOST_BEATS) { "a step lasts 1..$MOST_BEATS beats, got $beats" }
    }

    /** The step held to the end of the next bar of four: 1 and 3 become 4, 4 becomes 8. */
    public fun longer(): SongStep = copy(beats = minOf((beats / BAR + 1) * BAR, MOST_BEATS))

    /** The step held to the end of the bar before, or for one beat once it is a bar or less: 5 and 8 become 4, 4 becomes 1. */
    public fun shorter(): SongStep = copy(beats = maxOf((beats - 1) / BAR * BAR, 1))

    public companion object {
        // A bar of four: the beats a step is lengthened and shortened by.
        private const val BAR = 4

        // A thousand beats is past eight minutes at 120 BPM: longer than any one part of a song.
        public const val MOST_BEATS: Int = 999
    }
}
