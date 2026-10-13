package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * What the amp says when one of its controls moves, from its panel or over MIDI: a Hughes & Kettner
 * system exclusive carrying the [controller] number and the new [value].
 *
 * A knob's value runs 0..255, twice MIDI's resolution; a switch's and a selector's run 0..127.
 * The layout was read off a GrandMeister Deluxe 40 (firmware 1.16); docs/wmi-1.md has the captures.
 */
public data class ControlReport(
    val controller: Int,
    val value: Int,
) {
    public companion object {
        private val header = byteArrayOf(0x00, 0x20, 0x44, 0x00, 0x10, 0x00, 0x0A, 0x00, 0x04)

        /** The report [message] carries, or null when it is some other message or its checksum fails. */
        public fun parse(message: SystemExclusive): ControlReport? {
            val payload = message.payload
            if (payload.size != header.size + 4) return null
            if (!payload.copyOf(header.size).contentEquals(header)) return null
            val (controller, high, low, check) = payload.drop(header.size).map { it.toInt() }
            if (check != 0x0A xor controller xor high xor low) return null
            return ControlReport(controller, high shl 7 or low)
        }
    }
}
