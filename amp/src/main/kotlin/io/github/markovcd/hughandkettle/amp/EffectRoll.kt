package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import kotlin.random.Random

/**
 * New random values for the controls behind every effect that is on, and the changes that put back the
 * values they had. It switches no effect on or off: a [level][Control.level] never lands on 0, which the
 * amp stores as the effect off. A [kept] control, the time a tempo lock holds, keeps its value. A value
 * nobody knew is not put back.
 */
public data class EffectRoll(
    val effects: List<Control>,
    val changes: List<ControlChange>,
    val undo: List<ControlChange>,
) {
    public companion object {
        /** The effects of [state]'s amp that are on and have controls behind their switch, in panel order. */
        public fun effects(
            state: AmpState,
            kept: Control? = null,
        ): List<Control> =
            state.amp.controls.filter { switch ->
                switch.kind == ControlKind.Switch &&
                    switch.section == Section.Fx &&
                    state.values[switch.id]?.let(switch::isOn) == true &&
                    behind(state.amp, switch, kept).isNotEmpty()
            }

        /** A roll of every effect [state] has on, with values from [random]; null when none is on. */
        public fun of(
            state: AmpState,
            random: Random,
            kept: Control? = null,
        ): EffectRoll? {
            val effects = effects(state, kept).ifEmpty { return null }
            val rolled = effects.flatMap { behind(state.amp, it, kept) }
            return EffectRoll(
                effects = effects,
                changes = rolled.map { it.set(value(it, random)) },
                undo = rolled.mapNotNull { control -> state.values[control.id]?.let(control::set) },
            )
        }

        private fun behind(
            amp: AmpModel,
            switch: Control,
            kept: Control?,
        ): List<Control> = amp.controls.filter { it.needs == switch.id && it.id != kept?.id }

        private fun value(
            control: Control,
            random: Random,
        ): Int =
            when (val kind = control.kind) {
                is ControlKind.Selector -> Sectors.value(random.nextInt(kind.options.size), kind.options.size)
                ControlKind.Switch -> if (random.nextBoolean()) 127 else 0
                ControlKind.Knob -> random.nextInt(if (control.level) 1 else 0, 128)
            }
    }
}
