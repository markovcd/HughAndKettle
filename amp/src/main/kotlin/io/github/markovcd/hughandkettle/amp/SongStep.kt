package io.github.markovcd.hughandkettle.amp

/**
 * One step of a [Song], held for [beats]: either the amp's [preset], counted from 1, or the [tone] of that
 * name, kept on the phone. A tone step names its tone; the tone itself may since have been deleted.
 */
public data class SongStep(
    val preset: Int? = null,
    val beats: Int,
    val tone: String? = null,
) {
    init {
        require(beats in 1..MOST_BEATS) { "a step lasts 1..$MOST_BEATS beats, got $beats" }
        require((preset == null) != (tone == null)) { "a step is a preset or a tone, got $preset and $tone" }
        require(tone == null || tone.isNotBlank()) { "a tone step needs the tone's name" }
    }

    /** The step held to the end of the next bar of four: 1 and 3 become 4, 4 becomes 8. */
    public fun longer(): SongStep = copy(beats = minOf((beats / BAR + 1) * BAR, MOST_BEATS))

    /** The step held to the end of the bar before, or for one beat once it is a bar or less: 5 and 8 become 4, 4 becomes 1. */
    public fun shorter(): SongStep = copy(beats = maxOf((beats - 1) / BAR * BAR, 1))

    /** Whether this is a step on the tone [name], regardless of case. */
    public fun isTone(name: String): Boolean = tone.equals(name, ignoreCase = true)

    public companion object {
        // A bar of four: the beats a step is lengthened and shortened by.
        private const val BAR = 4

        // A thousand beats is past eight minutes at 120 BPM: longer than any one part of a song.
        public const val MOST_BEATS: Int = 999
    }
}
