package io.github.markovcd.hughandkettle.app

import kotlin.math.roundToInt

/** The geometry and the gesture of a knob, as numbers: 0..127 over a 270° sweep, turned by a straight drag. */
internal object KnobDial {
    const val MAX = 127
    const val START_DEGREES = 135f
    const val SWEEP_DEGREES = 270f

    /** One value per 1.5 dp: a full sweep is one thumb stroke of 190 dp. */
    const val DP_PER_STEP = 1.5f

    /** After holding still before the drag: one value per 6 dp, for the last few steps of gain. */
    const val FINE_DP_PER_STEP = 6f

    /** Where a tick can be felt without looking: the ends and noon. */
    val TickValues: Set<Int> = setOf(0, 64, MAX)

    fun angleDegrees(value: Int): Float = START_DEGREES + SWEEP_DEGREES * value / MAX

    /** The value after dragging [dxDp] right and [dyDp] down from where the drag started; up and right turn up. */
    fun valueAfterDrag(
        startValue: Int,
        dxDp: Float,
        dyDp: Float,
        fine: Boolean,
    ): Int {
        val steps = ((dxDp - dyDp) / if (fine) FINE_DP_PER_STEP else DP_PER_STEP).roundToInt()
        return (startValue + steps).coerceIn(0, MAX)
    }

    /** Whether moving from [from] to [to] arrives at or passes a tick value. */
    fun reachesTick(
        from: Int,
        to: Int,
    ): Boolean {
        if (from == to) return false
        val range = minOf(from, to)..maxOf(from, to)
        return TickValues.any { it in range && it != from }
    }
}
