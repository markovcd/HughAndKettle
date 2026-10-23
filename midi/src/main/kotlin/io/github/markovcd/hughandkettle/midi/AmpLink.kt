package io.github.markovcd.hughandkettle.midi

import java.io.Closeable

/**
 * An open way to the amp, over a WMI-1 or a MIDI cable: every message goes out whole. What the amp sends
 * goes to whatever the link was opened with.
 */
public interface AmpLink : Closeable {
    public fun send(message: MidiMessage): Result<Unit>
}
