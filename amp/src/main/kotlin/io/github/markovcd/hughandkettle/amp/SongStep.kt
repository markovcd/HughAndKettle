package io.github.markovcd.hughandkettle.amp

/** One step of a [Song]: the amp's [preset], counted from 1, held for [beats]. */
public data class SongStep(
    val preset: Int,
    val beats: Int,
) {
    init {
        require(beats in 1..MOST_BEATS) { "a step lasts 1..$MOST_BEATS beats, got $beats" }
    }

    public companion object {
        // A thousand beats is past eight minutes at 120 BPM: longer than any one part of a song.
        public const val MOST_BEATS: Int = 999
    }
}
