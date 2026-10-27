package io.github.markovcd.hughandkettle.amp

/** How one preset of a [LoudnessMatch] ended. */
public sealed interface MatchResult {
    /** The preset, counted from 1. */
    public val preset: Int

    /** The volume stored with the preset, once the amp has played it. */
    public val stored: Int?

    /** The preset the others were matched to, played at [volume] with [correction]. */
    public data class Reference(
        override val preset: Int,
        override val stored: Int,
        val volume: Int,
        val correction: Int,
    ) : MatchResult

    /**
     * The preset brought to [volume], [off] dB from the reference, by [correction]. Not [reached] when the
     * volume knob ran out at either end first.
     */
    public data class Matched(
        override val preset: Int,
        override val stored: Int,
        val volume: Int,
        val correction: Int,
        val off: Double,
        val reached: Boolean,
    ) : MatchResult

    /** The preset left as it was. */
    public data class Skipped(
        override val preset: Int,
        override val stored: Int?,
    ) : MatchResult
}
