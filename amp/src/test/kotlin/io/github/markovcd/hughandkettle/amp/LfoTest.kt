package io.github.markovcd.hughandkettle.amp

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LfoTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val volume = amp.control("reverb_volume")!!
    private val type = amp.control("modulation_type")!!

    /** The values [lfo] is at over one second, a quarter of a second apart. */
    private fun heard(lfo: Lfo): List<Int> = (0..4).map { lfo.value((it * 250).milliseconds) }

    @Test
    fun `Each shape swings from its start between the two ends at its rate`() {
        val cases =
            mapOf(
                LfoShape.Sine to listOf(20, 60, 100, 60, 20),
                LfoShape.Triangle to listOf(20, 60, 100, 60, 20),
                LfoShape.Square to listOf(100, 100, 20, 20, 100),
                LfoShape.RampUp to listOf(20, 40, 60, 80, 20),
                LfoShape.RampDown to listOf(100, 80, 60, 40, 100),
            )
        cases.forEach { (shape, values) ->
            assertEquals(values, heard(Lfo(volume, shape, hz = 1.0, low = 20, high = 100)), "$shape")
        }
    }

    @Test
    fun `Ends given high first swing over the same range`() {
        assertEquals(
            heard(Lfo(volume, LfoShape.RampUp, hz = 1.0, low = 20, high = 100)),
            heard(Lfo(volume, LfoShape.RampUp, hz = 1.0, low = 100, high = 20)),
        )
    }

    @Test
    fun `Random holds one level a cycle, inside the ends, the same for the same seed`() {
        val lfo = Lfo(volume, LfoShape.Random, hz = 2.0, low = 30, high = 90, seed = 7)
        val cycles = (0 until 20).map { lfo.value((it * 500).milliseconds) }
        assertTrue(cycles.all { it in 30..90 }, "$cycles")
        assertTrue(cycles.toSet().size > 1, "$cycles")
        (0 until 20).forEach { assertEquals(cycles[it], lfo.value((it * 500 + 400).milliseconds), "cycle $it") }
        assertEquals(cycles, (0 until 20).map { lfo.copy().value((it * 500).milliseconds) })
    }

    @Test
    fun `A knob swings either side of where it is, a selector across the option it is on`() {
        val knob = Lfo.around(volume, 100)
        assertEquals(68 to 127, knob.low to knob.high)
        val selector = Lfo.around(type, 80)
        assertEquals(64 to 95, selector.low to selector.high)
        assertFalse(selector.crosses)
        assertTrue(selector.copy(high = 100).crosses)
        assertFalse(knob.copy(low = 0).crosses)
    }

    @Test
    fun `Past five swings a second the rate may sound stepped`() {
        assertFalse(Lfo(volume, hz = 5.0, low = 0, high = 127).stepped)
        assertTrue(Lfo(volume, hz = 6.0, low = 0, high = 127).stepped)
    }

    @Test
    fun `A switch, an end past 127 or a rate past ten a second is refused`() {
        assertFailsWith<IllegalArgumentException> { Lfo(amp.control("reverb")!!, low = 0, high = 127) }
        assertFailsWith<IllegalArgumentException> { Lfo(volume, low = 0, high = 128) }
        assertFailsWith<IllegalArgumentException> { Lfo(volume, hz = 11.0, low = 0, high = 127) }
    }

    @Test
    fun `Every rate a rate knob offers is one an LFO takes, slowest first`() {
        assertEquals(Lfo.RATES.sorted(), Lfo.RATES)
        assertEquals(Lfo.SLOWEST_HZ to Lfo.FASTEST_HZ, Lfo.RATES.first() to Lfo.RATES.last())
        Lfo.RATES.forEach { Lfo(volume, hz = it, low = 0, high = 127) }
    }

    @Test
    fun `A rolled LFO keeps its control, a level off 0 and a selector inside one option`() {
        (1..200).forEach { seed ->
            val level = Lfo.around(volume, 50).rolled(Random(seed))
            assertEquals(volume, level.control)
            assertTrue(level.low in 1..127 && level.high in level.low..127, "seed $seed: $level")
            assertTrue(level.hz in Lfo.RATES, "seed $seed: $level")
            assertFalse(Lfo.around(type, 50).rolled(Random(seed)).crosses, "seed $seed")
        }
        assertEquals(Lfo.around(volume, 50).rolled(Random(3)), Lfo.around(volume, 50).rolled(Random(3)))
        assertTrue((1..50).map { Lfo.around(volume, 50).rolled(Random(it)).shape }.toSet().size > 1)
    }

    @Test
    fun `Every shape is found by its id`() {
        LfoShape.entries.forEach { assertEquals(it, LfoShape.byId(it.id)) }
        assertEquals(null, LfoShape.byId("saw"))
    }

    @Test
    fun `A long run keeps its phase`() {
        val lfo = Lfo(volume, LfoShape.RampUp, hz = 0.5, low = 0, high = 100)
        assertEquals(50, lfo.value(3.hoursAnd(1.seconds)))
    }

    private fun Int.hoursAnd(extra: Duration): Duration = (this * 3600).seconds + extra
}
