package io.github.markovcd.hughandkettle.midi

/** A system exclusive message; [payload] is what sits between F0 and F7. */
public class SystemExclusive(
    payload: ByteArray,
) : MidiMessage {
    public val payload: ByteArray = payload.copyOf()

    init {
        require(payload.all { it >= 0 }) { "a system exclusive payload holds data bytes only" }
    }

    override fun encode(): ByteArray = byteArrayOf(0xF0.toByte()) + payload + byteArrayOf(0xF7.toByte())

    override fun equals(other: Any?): Boolean = other is SystemExclusive && payload.contentEquals(other.payload)

    override fun hashCode(): Int = payload.contentHashCode()

    override fun toString(): String = "SystemExclusive(${payload.toHex()})"
}
