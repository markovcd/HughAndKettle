package io.github.markovcd.hughandkettle.midi

/** One complete MIDI 1.0 message, as the amp or the app sends it. */
public sealed interface MidiMessage {
    /** The bytes on the wire, always with a status byte: the link never relies on running status. */
    public fun encode(): ByteArray
}
