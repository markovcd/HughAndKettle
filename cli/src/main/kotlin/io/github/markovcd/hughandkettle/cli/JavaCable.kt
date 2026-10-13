package io.github.markovcd.hughandkettle.cli

import javax.sound.midi.MidiDevice

/**
 * A MIDI cable as Java's MIDI system sees it: one name, a device that takes messages to the amp and
 * one that gives what the amp sends, either of which a cable may lack.
 */
internal class JavaCable(
    val name: String,
    val description: String,
    val toAmp: MidiDevice?,
    val fromAmp: MidiDevice?,
)
