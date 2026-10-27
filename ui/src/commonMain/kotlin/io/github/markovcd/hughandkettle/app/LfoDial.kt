package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Lfo
import kotlin.math.abs
import kotlin.math.roundToInt

/** How a rate knob's 0..127 steps through [Lfo.RATES], slowest at 0. */
internal object LfoDial {
    private val last = Lfo.RATES.lastIndex

    fun hz(value: Int): Double =
        Lfo.RATES[(value.coerceIn(0, KnobDial.MAX) * last / KnobDial.MAX.toDouble()).roundToInt()]

    /** The knob value of the rate nearest [hz]. */
    fun value(hz: Double): Int {
        val index = Lfo.RATES.indices.minBy { abs(Lfo.RATES[it] - hz) }
        return (index * KnobDial.MAX / last.toDouble()).roundToInt()
    }
}
