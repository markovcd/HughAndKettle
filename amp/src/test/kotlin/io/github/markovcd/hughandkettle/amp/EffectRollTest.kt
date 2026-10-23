package io.github.markovcd.hughandkettle.amp

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EffectRollTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val time = amp.control("delay_time")!!

    /** The amp with modulation and delay on, reverb off and every effect knob at 64. */
    private val state =
        AmpState(
            amp,
            amp.controls.filter { it.section == Section.Fx }.associate { it.id to 64 } +
                mapOf("reverb" to 0, "fx_loop" to 0, "gain" to 30),
        )

    /** The ids of the controls [roll] changes. */
    private fun changed(roll: EffectRoll?) =
        roll!!.changes.map { change ->
            amp.controls
                .first {
                    it.controller ==
                        change.controller
                }.id
        }

    @Test
    fun `Only the controls behind an effect that is on change`() {
        assertEquals(
            listOf(
                "modulation_type",
                "modulation_intensity",
                "modulation_speed",
                "delay_time",
                "delay_feedback",
                "delay_volume",
            ),
            changed(EffectRoll.of(state, Random(1))),
        )
    }

    @Test
    fun `The effects rolled are the ones on, in panel order`() {
        assertEquals(listOf("modulation", "delay"), EffectRoll.of(state, Random(1))!!.effects.map { it.id })
    }

    @Test
    fun `A kept time keeps its value while the rest of its effect changes`() {
        assertEquals(
            listOf("modulation_type", "modulation_intensity", "modulation_speed", "delay_feedback", "delay_volume"),
            changed(EffectRoll.of(state, Random(1), kept = time)),
        )
    }

    @Test
    fun `With every effect off there is nothing to roll`() {
        val off = AmpState(amp, mapOf("modulation" to 0, "delay" to 0, "reverb" to 0, "fx_loop" to 127))
        assertNull(EffectRoll.of(off, Random(1)))
        assertTrue(EffectRoll.effects(off).isEmpty())
    }

    @Test
    fun `An effect whose switch nobody knows is left alone`() {
        assertNull(EffectRoll.of(AmpState(amp, mapOf("delay_time" to 40)), Random(1)))
    }

    @Test
    fun `A level never lands on 0, so no effect is switched off`() {
        val levels =
            amp.controls
                .filter { it.level }
                .map { it.controller }
                .toSet()
        val values =
            (0 until 2000).flatMap { seed ->
                EffectRoll
                    .of(state, Random(seed))!!
                    .changes
                    .filter { it.controller in levels }
                    .map { it.value }
            }
        assertTrue(values.all { it in 1..127 }, "a level of 0 among ${values.filter { it == 0 }.size} rolls")
    }

    @Test
    fun `A modulation type lands in the middle of one of its sectors`() {
        val type = amp.control("modulation_type")!!
        val middles = (0 until 4).map { Sectors.value(it, 4) }.toSet()
        val values =
            (0 until 200).map { seed ->
                EffectRoll
                    .of(state, Random(seed))!!
                    .changes
                    .first { it.controller == type.controller }
                    .value
            }
        assertEquals(middles, values.toSet())
    }

    @Test
    fun `Undo puts back every value the roll changed`() {
        val roll = EffectRoll.of(state, Random(7))!!
        val rolled = roll.changes.fold(state) { before, change -> before.after(change) }
        assertEquals(state, roll.undo.fold(rolled) { before, change -> before.after(change) })
    }

    @Test
    fun `The same seed rolls the same values`() {
        assertEquals(EffectRoll.of(state, Random(3)), EffectRoll.of(state, Random(3)))
    }
}
