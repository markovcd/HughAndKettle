package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.SystemExclusive

/**
 * What the amp says after a channel switch, a preset recall or a store: a Hughes & Kettner system
 * exclusive carrying the [values] it is set to, keyed by controller, knobs in 0..255 and switches and
 * the power soak in 0..127, the [preset] counted from 1 when the panel matches a stored one, and the
 * [channel] it is on, 0 for Clean to 3 for Ultra. The effect switches are not in it.
 *
 * The layout was read off a GrandMeister Deluxe 40 (firmware 1.16); docs/wmi-1.md has the captures
 * and the bytes not yet decoded.
 */
public data class ValueBlock(
    val values: Map<Int, Int>,
    val preset: Int?,
    val channel: Int = 0,
) {
    /**
     * The block as the amp sends it, for a stand-in amp. A controller the block has no pair for is left
     * out, the pairs nobody has decoded are 0, a switch left out is off and a power soak left out is at full.
     */
    public fun message(): SystemExclusive {
        val mark = if (preset == null) listOf(0x7F, 0x7F, 0) else listOf(0, 0, preset - 1)
        val pairs =
            (controllers.map { values[it] ?: 0 } + List(PAIRS - controllers.size) { 0 }).flatMap {
                listOf(it shr 7, it and 0x7F)
            }
        val soak = values[POWER_SOAK]?.let { Sectors.index(it, SOAK_STEPS) } ?: (SOAK_STEPS - 1)
        val switches = bit(NOISE_GATE, NOISE_GATE_BIT)
        val flags = channel or bit(BOOST, BOOST_BIT) or bit(FX_LOOP, FX_LOOP_BIT) or (soak shl SOAK_SHIFT)
        val body = mark + pairs + listOf(switches, flags)
        val check = body.fold(0x4E) { check, byte -> check xor byte }
        return SystemExclusive((header.toList() + (body + check).map { it.toByte() }).toByteArray())
    }

    private fun bit(
        controller: Int,
        bit: Int,
    ): Int = if ((values[controller] ?: 0) >= ON / 2) bit else 0

    public companion object {
        private val header = byteArrayOf(0x00, 0x20, 0x44, 0x00, 0x10, 0x00, 0x0A, 0x00, 0x40)

        /** The controller each value pair holds, in order. */
        private val controllers = listOf(56, 21, 22, 57, 23, 24, 25, 29, 28, 4, 27, 1, 12)

        private const val PAIRS = 15

        private const val ON = 127
        private const val BOOST = 64
        private const val FX_LOOP = 55
        private const val NOISE_GATE = 63
        private const val POWER_SOAK = 30

        // The flags byte: the channel, boost (kept per channel), the FX loop, and the power soak's step
        // from Speaker Off (0) to full (4).
        private const val CHANNEL_BITS = 0x03
        private const val BOOST_BIT = 0x04
        private const val FX_LOOP_BIT = 0x08
        private const val SOAK_SHIFT = 4
        private const val SOAK_STEPS = 5

        // The byte ahead of the flags.
        private const val NOISE_GATE_BIT = 0x01

        // Preset marker (two bytes) and preset, 15 value pairs, the switches and flags, and the check.
        private const val SIZE = 9 + 3 + 2 * PAIRS + 2 + 1

        /** The block [message] carries, or null when it is some other message or its check fails. */
        public fun parse(message: SystemExclusive): ValueBlock? {
            val payload = message.payload.map { it.toInt() }
            if (payload.size != SIZE) return null
            if (payload.take(header.size) != header.map { it.toInt() }) return null
            val body = payload.subList(header.size, SIZE - 1)
            if (payload.last() != body.fold(0x4E) { check, byte -> check xor byte }) return null
            val (markHigh, markLow, preset) = body
            val (switches, flags) = body.takeLast(2)
            val soak = flags shr SOAK_SHIFT
            val knobs =
                controllers.withIndex().associate { (pair, controller) ->
                    controller to
                        (body[3 + 2 * pair] shl 7 or body[4 + 2 * pair])
                }
            val values =
                knobs +
                    mapOf(
                        BOOST to on(flags and BOOST_BIT),
                        FX_LOOP to on(flags and FX_LOOP_BIT),
                        NOISE_GATE to on(switches and NOISE_GATE_BIT),
                    ) +
                    // A step past full is not one the amp has been seen to send, so it names no power soak.
                    listOfNotNull(soak.takeIf { it < SOAK_STEPS }?.let { POWER_SOAK to Sectors.value(it, SOAK_STEPS) })
            return ValueBlock(values, if (markHigh == 0 && markLow == 0) preset + 1 else null, flags and CHANNEL_BITS)
        }

        private fun on(bit: Int): Int = if (bit != 0) ON else 0
    }
}
