package io.github.markovcd.hughandkettle.amp

import kotlin.math.PI
import kotlin.math.cos

/** How an [Lfo] swings over one cycle. [id] is what `hk lfo --shape` takes and is never renamed. */
public enum class LfoShape(
    public val id: String,
) {
    Sine("sine"),
    Triangle("triangle"),
    Square("square"),
    RampUp("ramp_up"),
    RampDown("ramp_down"),

    /** A new level each cycle, held until the next. */
    Random("random"),
    ;

    /** Where in its range a cycle at [phase] (0 until 1) is, from 0 at the low end to 1 at the high; [Random] takes [chance]. */
    internal fun level(
        phase: Double,
        chance: Double,
    ): Double =
        when (this) {
            Sine -> (1 - cos(2 * PI * phase)) / 2
            Triangle -> if (phase < HALF) 2 * phase else 2 - 2 * phase
            Square -> if (phase < HALF) 1.0 else 0.0
            RampUp -> phase
            RampDown -> 1 - phase
            Random -> chance
        }

    public companion object {
        private const val HALF = 0.5

        public fun byId(id: String): LfoShape? = entries.firstOrNull { it.id == id }
    }
}
