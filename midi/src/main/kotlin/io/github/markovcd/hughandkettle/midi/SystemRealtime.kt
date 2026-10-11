package io.github.markovcd.hughandkettle.midi

/** A one-byte real-time message (clock, start, stop, active sensing, reset); it may arrive inside any other message. */
public data class SystemRealtime(
    val status: Int,
) : MidiMessage {
    init {
        require(status in 0xF8..0xFF) { "real-time status must be F8..FF, got ${status.toString(16)}" }
    }

    override fun encode(): ByteArray = byteArrayOf(status.toByte())
}
