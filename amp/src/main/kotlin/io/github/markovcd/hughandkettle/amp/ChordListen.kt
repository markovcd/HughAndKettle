package io.github.markovcd.hughandkettle.amp

import kotlin.math.log10
import kotlin.math.pow

/**
 * Listening for one chord, held and let ring, and how loud it rang. Frames of [LevelFrame.LENGTH] come in
 * through [after]; the quiet before the chord sets the room's floor, a rise well above it is the strum, and
 * the level is the mean power of the ring after the attack.
 */
@ConsistentCopyVisibility
public data class ChordListen private constructor(
    private val quiet: List<Double> = emptyList(),
    private val waited: Int = 0,
    private val sinceStrum: Int? = null,
    private val ring: List<Double> = emptyList(),
    /** The level of the last frame heard, for a meter. */
    val now: Double = LevelFrame.SILENCE,
    /** How the listen ended, or null while it goes on. */
    val outcome: Outcome? = null,
) {
    /** How a listen ended. */
    public sealed interface Outcome {
        /** The chord rang at [level], in dB below full scale. */
        public data class Measured(
            val level: Double,
        ) : Outcome

        /** Nothing could be measured, for [why]. */
        public data class Retry(
            val why: Why,
        ) : Outcome
    }

    /** Why a chord could not be measured. */
    public enum class Why {
        /** No strum was heard, or the chord died before it could be measured. */
        TooQuiet,

        /** The chord clipped the microphone. */
        TooLoud,

        /** The room was too loud around the chord to tell it apart. */
        TooNoisy,
    }

    /** The room's level before the strum: the median of the last quiet frames. */
    private val floor: Double get() = quiet.sorted().let { if (it.isEmpty()) LevelFrame.SILENCE else it[it.size / 2] }

    /** The listen after one more [frame]. */
    public fun after(frame: LevelFrame): ChordListen {
        if (outcome != null) return this
        val heard = copy(now = frame.level, waited = waited + 1)
        val strummed = sinceStrum ?: return heard.waiting(frame)
        if (frame.peak >= CLIPPED) return heard.ended(Outcome.Retry(Why.TooLoud))
        if (strummed + 1 <= ATTACK) return heard.copy(sinceStrum = strummed + 1)
        if (frame.level < floor + DIED) {
            return heard.ended(if (ring.size >= SHORTEST) measured() else Outcome.Retry(Why.TooQuiet))
        }
        val rang = heard.copy(sinceStrum = strummed + 1, ring = ring + frame.level)
        return if (rang.ring.size >= HOLD) rang.ended(rang.measured()) else rang
    }

    private fun waiting(frame: LevelFrame): ChordListen =
        when {
            quiet.size >= SETTLE && frame.level >= floor + RISE && frame.level >= QUIETEST -> {
                if (frame.peak >= CLIPPED) ended(Outcome.Retry(Why.TooLoud)) else copy(sinceStrum = 1)
            }

            waited >= WAIT -> {
                ended(Outcome.Retry(if (floor >= NOISY) Why.TooNoisy else Why.TooQuiet))
            }

            else -> {
                copy(quiet = (quiet + frame.level).takeLast(SETTLE))
            }
        }

    private fun measured(): Outcome {
        val level = 10 * log10(ring.sumOf { 10.0.pow(it / 10) } / ring.size)
        return if (level < floor + CLEAR) Outcome.Retry(Why.TooNoisy) else Outcome.Measured(level)
    }

    private fun ended(outcome: Outcome): ChordListen = copy(outcome = outcome)

    public companion object {
        /** A listen that has heard nothing yet. */
        public fun start(): ChordListen = ChordListen()

        // Frames of the room heard before a strum counts: half a second.
        private const val SETTLE = 10

        // How far above the floor a frame has to jump to be a strum, and the softest strum heard at all.
        private const val RISE = 10.0
        private const val QUIETEST = -60.0

        // Frames of the strum's attack left out of the level: a quarter of a second.
        private const val ATTACK = 5

        // Frames of ring measured: a second and a half; fewer than half a second is too short to trust.
        private const val HOLD = 30
        private const val SHORTEST = 10

        // A ring this close to the floor has died; one that ends this close to it was mostly room.
        private const val DIED = 6.0
        private const val CLEAR = 15.0

        // A peak this near full scale has clipped the microphone.
        private const val CLIPPED = 0.98

        // Frames waited for a strum: ten seconds; a floor this loud is a room too noisy to measure in.
        private const val WAIT = 200
        private const val NOISY = -40.0
    }
}
