package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel

/**
 * A named set of control values for one amp model, kept on the phone or by `hk` rather than in one of
 * the amp's presets. It holds only the values that were known when it was saved.
 */
public data class Tone(
    val name: String,
    val amp: AmpModel,
    val values: Map<String, Int>,
) {
    init {
        require(name.isNotBlank()) { "a tone needs a name" }
        val unknown = values.keys.filter { amp.control(it) == null }
        require(unknown.isEmpty()) { "${amp.name} has no controls $unknown" }
        val outside = values.filterValues { it !in 0..127 }
        require(outside.isEmpty()) { "values must be 0..127, got $outside" }
    }

    /** The messages that recall this tone: one control change per value, in the amp's panel order. */
    public fun recall(channel: MidiChannel = MidiChannel.Default): List<ControlChange> =
        amp.controls.mapNotNull { control -> values[control.id]?.let { control.set(it, channel) } }

    public companion object {
        /** The tone [name] holding every value [state] knows. */
        public fun of(
            name: String,
            state: AmpState,
        ): Tone = Tone(name, state.amp, state.values)
    }
}
