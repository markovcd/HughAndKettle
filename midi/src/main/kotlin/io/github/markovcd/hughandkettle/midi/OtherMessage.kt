package io.github.markovcd.hughandkettle.midi

/** A complete message of a kind the amp does not use (a note, pitch bend, song position). Kept so a log shows it. */
public class OtherMessage(
    bytes: ByteArray,
) : MidiMessage {
    public val bytes: ByteArray = bytes.copyOf()

    override fun encode(): ByteArray = bytes.copyOf()

    override fun equals(other: Any?): Boolean = other is OtherMessage && bytes.contentEquals(other.bytes)

    override fun hashCode(): Int = bytes.contentHashCode()

    override fun toString(): String = "OtherMessage(${bytes.toHex()})"
}
