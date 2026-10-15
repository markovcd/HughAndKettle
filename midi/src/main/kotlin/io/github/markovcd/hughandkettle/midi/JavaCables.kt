package io.github.markovcd.hughandkettle.midi

import javax.sound.midi.MidiSystem
import javax.sound.midi.Sequencer
import javax.sound.midi.ShortMessage
import javax.sound.midi.Synthesizer
import javax.sound.midi.SysexMessage
import javax.sound.midi.MidiMessage as JavaMidiMessage

/** The MIDI cables plugged into this machine, through Java's own MIDI, and messages spelled its way. */
public object JavaCables {
    /** Every MIDI port that is not Java's own synthesizer or sequencer, the two directions of one name together. */
    public fun all(): Result<List<JavaCable>> =
        runCatching {
            MidiSystem
                .getMidiDeviceInfo()
                .map(MidiSystem::getMidiDevice)
                .filter { it !is Synthesizer && it !is Sequencer }
                .groupBy { it.deviceInfo.name }
                .map { (name, devices) ->
                    JavaCable(
                        name,
                        devices.first().deviceInfo.description,
                        toAmp = devices.firstOrNull { it.maxReceivers != 0 },
                        fromAmp = devices.firstOrNull { it.maxTransmitters != 0 },
                    )
                }
        }

    /** The first cable whose name or description holds [part], ignoring case. */
    public fun find(part: String): Result<JavaCable?> =
        all().map { cables ->
            cables.firstOrNull { cable ->
                listOf(cable.name, cable.description).any { it.contains(part, ignoreCase = true) }
            }
        }

    /** [message] as Java's MIDI spells it, with the same bytes. */
    public fun toJava(message: MidiMessage): JavaMidiMessage {
        val bytes = message.encode()
        val status = bytes[0].toInt() and 0xFF
        if (status == SYSTEM_EXCLUSIVE) return SysexMessage(bytes, bytes.size)
        val data = bytes.drop(1).map { it.toInt() and 0xFF }
        return ShortMessage().apply {
            when (data.size) {
                0 -> setMessage(status)
                1 -> setMessage(status, data[0], 0)
                else -> setMessage(status, data[0], data[1])
            }
        }
    }

    private const val SYSTEM_EXCLUSIVE = 0xF0
}
