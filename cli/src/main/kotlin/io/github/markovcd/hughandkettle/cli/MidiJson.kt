package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.OtherMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import io.github.markovcd.hughandkettle.midi.SystemRealtime
import io.github.markovcd.hughandkettle.midi.toHex

/** How a MIDI message is spelled on the CLI's output. */
public object MidiJson {
    public fun fields(message: MidiMessage): List<Pair<String, Any?>> =
        listOf("hex" to message.encode().toHex()) +
            when (message) {
                is ControlChange -> {
                    listOf(
                        "type" to "control_change",
                        "channel" to message.channel.number,
                        "controller" to message.controller,
                        "value" to message.value,
                    )
                }

                is ProgramChange -> {
                    listOf(
                        "type" to "program_change",
                        "channel" to message.channel.number,
                        "program" to message.program,
                    )
                }

                is SystemExclusive -> {
                    listOf("type" to "system_exclusive")
                }

                is SystemRealtime -> {
                    listOf("type" to "system_realtime")
                }

                is OtherMessage -> {
                    listOf("type" to "other")
                }
            }
}
