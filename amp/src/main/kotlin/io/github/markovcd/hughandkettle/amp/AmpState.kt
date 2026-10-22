package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive

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
     * The state after [message]. A control change or the amp's [ControlReport] sets its control, and
     * the amp's [ValueBlock] sets its channel and every value it carries. The block carries no effect
     * switch, so one still unknown is taken from its [level][Control.level]. A program change recalls a
     * preset, whose stored values the app has not seen until the amp's block arrives, so every value
     * becomes unknown.
     * Anything else changes nothing.
     */
    public fun after(message: MidiMessage): AmpState =
        when (message) {
            is ControlChange -> {
                set(message.controller) { message.value }
            }

            is SystemExclusive -> {
                val report = ControlReport.parse(message)
                val block = ValueBlock.parse(message)
                when {
                    report != null -> {
                        set(report.controller) { it.fromReport(report.value) }
                    }

                    block != null -> {
                        block.values.entries
                            .fold(onChannel(block.channel).copy(preset = block.preset ?: preset)) {
                                state,
                                (controller, value),
                                ->
                                state.set(controller) { it.fromReport(value) }
                            }.withEffectsFromLevels()
                    }

                    else -> {
                        this
                    }
                }
            }

            is ProgramChange -> {
                if (message.program < amp.presetCount) copy(values = emptyMap(), preset = message.program + 1) else this
            }

            else -> {
                this
            }
        }

    private fun onChannel(channel: Int): AmpState {
        val control = amp.control("channel") ?: return this
        return copy(values = values + (control.id to control.selected(channel).value))
    }

    private fun withEffectsFromLevels(): AmpState {
        val effects =
            amp.controls
                .filter { it.level && it.needs !in values }
                .mapNotNull { level -> values[level.id]?.let { level.needs!! to if (it > 0) 127 else 0 } }
        return copy(values = values + effects)
    }

    private fun set(
        controller: Int,
        value: (Control) -> Int,
    ): AmpState {
        val control = amp.controls.firstOrNull { it.controller == controller } ?: return this
        return copy(values = values + (control.id to value(control)))
    }
}
