package io.github.markovcd.hughandkettle.midi

/** A MIDI channel as a person counts them, 1 to 16. The wire carries it as 0 to 15. */
@JvmInline
public value class MidiChannel(
    public val number: Int,
) {
    init {
        require(number in 1..16) { "MIDI channel must be 1..16, got $number" }
    }

    internal val wire: Int get() = number - 1

    public companion object {
        public val Default: MidiChannel = MidiChannel(1)

        internal fun fromWire(low: Int): MidiChannel = MidiChannel(low + 1)
    }
}
