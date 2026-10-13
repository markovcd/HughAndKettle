package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * What the amp says after a channel switch, a preset recall or a store: a Hughes & Kettner system
 * exclusive carrying the knob [values] it is set to, keyed by controller, in 0..255, and the [preset]
 * counted from 1 when the panel matches a stored one.
 *
 * The layout was read off a GrandMeister Deluxe 40 (firmware 1.16); docs/wmi-1.md has the captures
 * and the bytes not yet decoded.
 */
public data class ValueBlock(
    val values: Map<Int, Int>,
    val preset: Int?,
) {
    public companion object {
        private val header = byteArrayOf(0x00, 0x20, 0x44, 0x00, 0x10, 0x00, 0x0A, 0x00, 0x40)

        /** The controller each value pair holds, in order. */
        private val controllers = listOf(56, 21, 22, 57, 23, 24, 25, 29, 28, 4, 27, 1, 12)

        // Preset marker (two bytes) and preset, 15 value pairs, a high byte and flags, and the check.
        private const val SIZE = 9 + 3 + 30 + 2 + 1

        /** The block [message] carries, or null when it is some other message or its check fails. */
        public fun parse(message: SystemExclusive): ValueBlock? {
            val payload = message.payload.map { it.toInt() }
            if (payload.size != SIZE) return null
            if (payload.take(header.size) != header.map { it.toInt() }) return null
            val body = payload.subList(header.size, SIZE - 1)
            if (payload.last() != body.fold(0x4E) { check, byte -> check xor byte }) return null
            val (markHigh, markLow, preset) = body
            val values =
                controllers.withIndex().associate { (pair, controller) ->
                    controller to
                        (body[3 + 2 * pair] shl 7 or body[4 + 2 * pair])
                }
            return ValueBlock(values, if (markHigh == 0 && markLow == 0) preset + 1 else null)
        }
    }
}
