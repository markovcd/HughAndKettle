package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel

/**
 * One thing on the amp the app can turn or switch, and the controller number that moves it.
 *
 * [id] is written into tone files and is never renamed once shipped; [name] is what a person reads.
 * [scale] is what the knob's value means on the amp, where the manual gives it. [needs] is the id of
 * the effect switch this control is heard through, so it does nothing while that switch is off.
 * [fine] says the amp [reports][ControlReport] it in 256 steps rather than 128. [level] says the amp
 * stores the effect it [needs] switched off as this knob at 0.
 */
public data class Control(
    val id: String,
    val name: String,
    val controller: Int,
    val kind: ControlKind,
    val section: Section,
    val scale: Scale? = null,
    val needs: String? = null,
    val fine: Boolean = kind == ControlKind.Knob,
    val level: Boolean = false,
) {
    init {
        require(id.matches(Regex("[a-z][a-z0-9_]*"))) { "control id must be snake_case, got '$id'" }
        require(controller in 0..127) { "controller must be 0..127, got $controller" }
        require(scale == null || kind == ControlKind.Knob) { "only a knob has a scale, and '$id' is not one" }
        require(!level || needs != null) { "a level is an effect's, and '$id' needs no effect" }
    }

    /** Whether this is a time knob, one whose scale is in milliseconds: it takes a tapped beat and a tempo lock. */
    val timed: Boolean get() = scale?.unit == "ms"

    /** The message that sets this control to [value] on [channel]. */
    public fun set(
        value: Int,
        channel: MidiChannel = MidiChannel.Default,
    ): ControlChange = ControlChange(channel, controller, value)

    /** The 0..127 value a [ControlReport] of [reported] stands for. */
    public fun fromReport(reported: Int): Int = (if (fine) reported / 2 else reported).coerceIn(0, 127)

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
