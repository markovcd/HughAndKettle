package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * A switch of the amp's sound: to its [preset], counted from 1, or to the [tone] of that name. [reported] when
 * the amp told of it after the fact, as it does for a recall from its footswitch, rather than the app sending it.
 */
public data class SoundSwitch(
    val preset: Int? = null,
    val tone: String? = null,
    val reported: Boolean = false,
) {
    init {
        require((preset == null) != (tone == null)) { "a switch is to a preset or a tone, got $preset and $tone" }
    }

    public companion object {
        /** The recall of a preset of [amp] that [message] from the amp reports, a program change or a value block naming one; else null. */
        public fun heard(
            message: MidiMessage,
            amp: AmpModel,
        ): SoundSwitch? {
            val preset =
                when (message) {
                    is ProgramChange -> (message.program + 1).takeIf { it <= amp.presetCount }
                    is SystemExclusive -> ValueBlock.parse(message)?.preset
                    else -> null
                }
            return preset?.let { SoundSwitch(it, reported = true) }
        }
    }
}
