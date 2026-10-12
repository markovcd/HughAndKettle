package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange

/**
 * One amp the app knows how to drive: its name, the controls its MIDI chart lists, and its presets,
 * grouped [presetsPerBank] to a bank where its manual says so.
 */
public data class AmpModel(
    val id: String,
    val name: String,
    val controls: List<Control>,
    val presetCount: Int = 128,
    val presetsPerBank: Int? = null,
) {
    init {
        val duplicateIds = controls.groupBy(Control::id).filterValues { it.size > 1 }.keys
        require(duplicateIds.isEmpty()) { "$name repeats control ids $duplicateIds" }
        val duplicateControllers = controls.groupBy(Control::controller).filterValues { it.size > 1 }.keys
        require(duplicateControllers.isEmpty()) { "$name repeats controller numbers $duplicateControllers" }
        require(presetCount in 1..128) { "$name has $presetCount presets; a program change reaches 1..128" }
        require(presetsPerBank == null || (presetsPerBank > 0 && presetCount % presetsPerBank == 0)) {
            "$name cannot split $presetCount presets into banks of $presetsPerBank"
        }
    }

    public fun control(id: String): Control? = controls.firstOrNull { it.id == id }

    /** The message that recalls [preset], counted from 1 as the amp shows it. */
    public fun recall(
        preset: Int,
        channel: MidiChannel = MidiChannel.Default,
    ): ProgramChange {
        require(preset in 1..presetCount) { "$name has presets 1..$presetCount, not $preset" }
        return ProgramChange(channel, preset - 1)
    }

    /** Where [preset] sits as the amp shows it, or null for an amp that numbers presets straight through. */
    public fun slotOf(preset: Int): PresetSlot? {
        require(preset in 1..presetCount) { "$name has presets 1..$presetCount, not $preset" }
        val perBank = presetsPerBank ?: return null
        return PresetSlot(bank = (preset - 1) / perBank + 1, slot = (preset - 1) % perBank + 1)
    }
}
