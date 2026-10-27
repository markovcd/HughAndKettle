package io.github.markovcd.hughandkettle.amp

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * The app standing in for an LFO the amp lacks: [control], a knob or a selector behind an effect, swept
 * between [low] and [high] (either order) by [shape], [hz] full swings a second. Whoever runs it sends the
 * [value] at most every [STEP]. [seed] picks the [LfoShape.Random] levels, the same for the same seed.
 */
public data class Lfo(
    val control: Control,
    val shape: LfoShape = LfoShape.Sine,
    val hz: Double = DEFAULT_HZ,
    val low: Int,
    val high: Int,
    val seed: Int = 0,
) {
    init {
        require(
            control.kind != ControlKind.Switch,
        ) { "an LFO sweeps a knob or a selector, and '${control.id}' is a switch" }
        require(low in 0..127 && high in 0..127) { "an LFO's ends are 0..127, got $low and $high" }
        require(hz in SLOWEST_HZ..FASTEST_HZ) { "an LFO's rate is $SLOWEST_HZ..$FASTEST_HZ Hz, got $hz" }
    }

    /** The control's value [elapsed] after the LFO started, at the low end at the start. */
    public fun value(elapsed: Duration): Int {
        val cycles = elapsed.inWholeNanoseconds / NANOS_PER_SECOND * hz
        val cycle = floor(cycles)
        val chance = Random(seed.toLong() * CYCLE_SPREAD + cycle.toLong()).nextDouble()
        val bottom = minOf(low, high)
        return bottom + (abs(high - low) * shape.level(cycles - cycle, chance)).roundToInt()
    }

    /** Whether the swing crosses from one of a selector's options into another. */
    public val crosses: Boolean
        get() {
            val options = (control.kind as? ControlKind.Selector)?.options?.size ?: return false
            return Sectors.index(low, options) != Sectors.index(high, options)
        }

    /** Whether the rate is past what [STEP] can draw smoothly, so the swing may sound stepped. */
    public val stepped: Boolean get() = hz > SMOOTH_UP_TO_HZ

    /**
     * This LFO on the same control with a shape, a rate and ends from [random]: a level's ends never reach 0,
     * which the amp stores as the effect off, and a selector's stay inside one option.
     */
    public fun rolled(random: Random): Lfo {
        val kind = control.kind
        val range =
            if (kind is ControlKind.Selector) {
                Sectors.range(random.nextInt(kind.options.size), kind.options.size)
            } else {
                (if (control.level) 1 else 0)..127
            }
        val ends = List(2) { random.nextInt(range.first, range.last + 1) }.sorted()
        return copy(
            shape = LfoShape.entries[random.nextInt(LfoShape.entries.size)],
            hz = RATES[random.nextInt(RATES.size)],
            low = ends[0],
            high = ends[1],
        )
    }

    public companion object {
        /** How often a change is sent at most: 25 a second, a gentle load on the WMI-1 and the amp. */
        public val STEP: Duration = 40.milliseconds

        public const val SLOWEST_HZ: Double = 0.05
        public const val FASTEST_HZ: Double = 10.0
        public const val DEFAULT_HZ: Double = 0.5

        // Five changes a cycle, at 25 a second.
        public const val SMOOTH_UP_TO_HZ: Double = 5.0

        /** The rates a rate knob steps through, slowest first. */
        public val RATES: List<Double> =
            listOf(
                0.05,
                0.06,
                0.08,
                0.1,
                0.12,
                0.15,
                0.2,
                0.25,
                0.3,
                0.4,
                0.5,
                0.6,
                0.8,
                1.0,
                1.2,
                1.5,
                2.0,
                2.5,
                3.0,
                4.0,
                5.0,
                6.0,
                8.0,
                10.0,
            )

        // How far apart two seeds start in the random levels: past any count of cycles a run reaches.
        private const val CYCLE_SPREAD = 1_000_003L
        private const val NANOS_PER_SECOND = 1e9

        // A knob's default swing is this far either side of where it is.
        private const val REACH = 32

        /**
         * A sine LFO on [control] around [value], where it is now: a knob swings [REACH] either side of it, a
         * selector across the option it is on, so the swing stays in one effect.
         */
        public fun around(
            control: Control,
            value: Int?,
        ): Lfo {
            val now = value ?: 64
            val kind = control.kind
            val range =
                if (kind is ControlKind.Selector) {
                    Sectors.range(Sectors.index(now, kind.options.size), kind.options.size)
                } else {
                    (now - REACH).coerceAtLeast(0)..(now + REACH).coerceAtMost(127)
                }
            return Lfo(control, low = range.first, high = range.last)
        }
    }
}
