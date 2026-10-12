package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange

/**
 * What an amp is set to, as far as the app knows: a value per control id and the current preset.
 * Immutable, and replaced whole after every message, whether the app sent it or heard it from the
 * amp. A value or preset nobody has sent or heard is absent rather than guessed.
 */
public data class AmpState(
    val amp: AmpModel,
    val values: Map<String, Int> = emptyMap(),
    val preset: Int? = null,
) {
    /** Whether [control] is known to do nothing now: the effect switch it [needs][Control.needs] is off. */
    public fun silent(control: Control): Boolean {
        val switch = amp.control(control.needs ?: return false) ?: return false
        val value = values[switch.id] ?: return false
        return !switch.isOn(value)
    }

    /**
     * The state after [message]. A control change sets its control; a program change recalls a preset,
     * whose stored values the app has not seen, so every value becomes unknown. Anything else changes nothing.
     */
    public fun after(message: MidiMessage): AmpState =
        when (message) {
            is ControlChange -> {
                val control = amp.controls.firstOrNull { it.controller == message.controller }
                if (control == null) this else copy(values = values + (control.id to message.value))
            }

            is ProgramChange -> {
                if (message.program < amp.presetCount) copy(values = emptyMap(), preset = message.program + 1) else this
            }

            else -> {
                this
            }
        }
}
