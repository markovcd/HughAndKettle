package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.midi.MidiMessage
import java.io.Closeable

/** An open USB MIDI cable: every message goes out whole to the amp's MIDI in. */
class CableLink(
    val cable: Cable,
    private val write: (ByteArray) -> Unit,
    private val release: () -> Unit,
) : Closeable {
    fun send(message: MidiMessage): Result<Unit> = runCatching { synchronized(this) { write(message.encode()) } }

    override fun close() = release()
}
