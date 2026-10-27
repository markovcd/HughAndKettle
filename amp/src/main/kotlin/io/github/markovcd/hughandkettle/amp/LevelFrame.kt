package io.github.markovcd.hughandkettle.amp

import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/** How loud one short frame of a microphone's sound was: its [level] in dB below full scale, and its [peak], 0..1. */
public data class LevelFrame(
    val level: Double,
    val peak: Double,
) {
    public companion object {
        /** How long a frame lasts; a listen counts time in frames. */
        public val LENGTH: Duration = 50.milliseconds

        /** The level given to silence, which has none. */
        public const val SILENCE: Double = -120.0

        /** The frame of the first [count] of [samples], 16-bit. */
        public fun of(
            samples: ShortArray,
            count: Int = samples.size,
        ): LevelFrame {
            require(count in 0..samples.size) { "$count samples of ${samples.size}" }
            var sum = 0.0
            var peak = 0.0
            for (i in 0 until count) {
                val sample = samples[i] / FULL_SCALE
                sum += sample * sample
                peak = max(peak, abs(sample))
            }
            val rms = if (count == 0) 0.0 else sqrt(sum / count)
            return LevelFrame(decibels(rms), peak.coerceAtMost(1.0))
        }

        private fun decibels(rms: Double): Double = if (rms <= 0.0) SILENCE else max(SILENCE, 20 * log10(rms))

        private const val FULL_SCALE = 32768.0
    }
}
