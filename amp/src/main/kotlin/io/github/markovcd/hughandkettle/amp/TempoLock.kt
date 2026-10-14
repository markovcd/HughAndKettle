package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * A time knob's [value] held across preset recalls and channel switches: each preset stores its own
 * delay time, so the amp's [ValueBlock] after a recall carries the preset's, and the lock answers it
 * with a change back to the held one. Setting the knob by hand, by a tap or from the amp's panel or
 * footswitch moves what is held. While nothing is held, the first value heard is.
 */
public data class TempoLock(
    val control: Control,
    val value: Int? = null,
) {
    /** The lock after [message], sent or heard. */
    public fun after(message: MidiMessage): TempoLock {
        val moved =
            when (message) {
                is ControlChange -> {
                    message.value.takeIf { message.controller == control.controller }
                }

                is SystemExclusive -> {
                    val report = ControlReport.parse(message)
                    when {
                        report != null -> {
                            report.value.takeIf { report.controller == control.controller }?.let(control::fromReport)
                        }

                        value == null -> {
                            ValueBlock
                                .parse(message)
                                ?.values
                                ?.get(control.controller)
                                ?.let(control::fromReport)
                        }

                        else -> {
                            null
                        }
                    }
                }

                else -> {
                    null
                }
            }
        return if (moved == null) this else copy(value = moved)
    }

    /** The change that puts the held time back after [message]: one only for a value block carrying another. */
    public fun restore(message: MidiMessage): ControlChange? {
        val held = value ?: return null
        val block = ValueBlock.parse(message as? SystemExclusive ?: return null) ?: return null
        val carried = block.values[control.controller]?.let(control::fromReport) ?: return null
        return if (carried == held) null else control.set(held)
    }

    /** [messages] with every change of the time made to the held one, so a tone recalled keeps the tempo. */
    public fun kept(messages: List<MidiMessage>): List<MidiMessage> {
        val held = value ?: return messages
        return messages.map {
            if (it is ControlChange &&
                it.controller == control.controller
            ) {
                it.copy(value = held)
            } else {
                it
            }
        }
    }
}
