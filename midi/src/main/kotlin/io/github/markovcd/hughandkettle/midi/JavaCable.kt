package io.github.markovcd.hughandkettle.midi

import javax.sound.midi.MidiDevice

/**
 * A MIDI cable as Java's MIDI system sees it: one name, a device that takes messages to the amp and
 * one that gives what the amp sends, either of which a cable may lack.
 */
public class JavaCable(
    public val name: String,
    public val description: String,
    public val toAmp: MidiDevice?,
    public val fromAmp: MidiDevice?,
)
