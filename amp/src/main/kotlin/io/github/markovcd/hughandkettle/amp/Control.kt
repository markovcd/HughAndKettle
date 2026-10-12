package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel

/**
 * One thing on the amp the app can turn or switch, and the controller number that moves it.
 *
 * [id] is written into tone files and is never renamed once shipped; [name] is what a person reads.
 * [scale] is what the knob's value means on the amp, where the manual gives it. [needs] is the id of
 * the effect switch this control is heard through, so it does nothing while that switch is off.
 */
public data class Control(
    val id: String,
    val name: String,
    val controller: Int,
    val kind: ControlKind,
    val section: Section,
    val scale: Scale? = null,
    val needs: String? = null,
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) { "control id must be snake_case, got '$id'" }
        require(controller in 0..127) { "controller must be 0..127, got $controller" }
        require(scale == null || kind == ControlKind.Knob) { "only a knob has a scale, and '$id' is not one" }
    }

    /** The message that sets this control to [value] on [channel]. */
    public fun set(
        value: Int,
        channel: MidiChannel = MidiChannel.Default,
    ): ControlChange = ControlChange(channel, controller, value)

    /** Whether a switch at [value] is on: the upper half of the range, as the amp reads it. */
    public fun isOn(value: Int): Boolean = value >= 64

    public fun switched(
        on: Boolean,
        channel: MidiChannel = MidiChannel.Default,
    ): ControlChange = set(if (on) 127 else 0, channel)

    /** The message that selects option [index] of a [ControlKind.Selector]. */
    public fun selected(
        index: Int,
        channel: MidiChannel = MidiChannel.Default,
    ): ControlChange {
        val selector = kind as? ControlKind.Selector ?: error("'$id' is not a selector")
        return set(Sectors.value(index, selector.options.size), channel)
    }
}
