package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * Reading the amp's presets into tones, one at a time: recall a preset, and take what the amp says
 * up to the [ValueBlock] naming that preset as its tone. The caller sends [next], hands on every
 * message it hears through [after], and calls [skip] when the block does not come in time.
 * A tone holds only what the amp reported, which leaves out the switches the block does not carry.
 */
public class PresetReading private constructor(
    public val names: PresetNames,
    private val left: List<Int>,
    private val heard: AmpState,
    public val tones: List<Tone>,
    public val missed: List<Int>,
) {
    public val amp: AmpModel get() = names.amp

    /** The preset being read, or null when every one is done. */
    public val preset: Int? get() = left.firstOrNull()

    /** How many presets are still to read, [preset] among them. */
    public val remaining: Int get() = left.size

    /** The program change that recalls [preset]. */
    public val next: ProgramChange? get() = preset?.let(amp::recall)

    /** The reading after the amp said [message]; the block naming [preset] ends that preset. */
    public fun after(message: MidiMessage): PresetReading {
        val preset = preset ?: return this
        val state = heard.after(message)
        val block = (message as? SystemExclusive)?.let(ValueBlock::parse)
        if (block?.preset != preset) return copy(heard = state)
        return copy(
            left = left.drop(1),
            heard = AmpState(amp),
            tones =
                tones + Tone(names.toneName(preset), amp, state.values),
        )
    }

    /** The reading with [preset] given up on. */
    public fun skip(): PresetReading {
        val preset = preset ?: return this
        return copy(left = left.drop(1), heard = AmpState(amp), missed = missed + preset)
    }

    private fun copy(
        left: List<Int> = this.left,
        heard: AmpState = this.heard,
        tones: List<Tone> = this.tones,
        missed: List<Int> = this.missed,
    ) = PresetReading(names, left, heard, tones, missed)

    public companion object {
        /** A reading of [presets], each counted from 1, named after [names]. */
        public fun of(
            names: PresetNames,
            presets: List<Int> = (1..names.amp.presetCount).toList(),
        ): PresetReading {
            val outside = presets.filter { it !in 1..names.amp.presetCount }
            require(outside.isEmpty()) { "${names.amp.name} has presets 1..${names.amp.presetCount}, not $outside" }
            return PresetReading(names, presets, AmpState(names.amp), emptyList(), emptyList())
        }
    }
}
