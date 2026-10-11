package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.MidiMessage
import java.net.InetSocketAddress

/** What a [FakeWmi] saw, in the order it saw it. */
public sealed interface FakeWmiEvent {
    public data class Opened(
        val from: InetSocketAddress,
    ) : FakeWmiEvent

    public data class Received(
        val message: MidiMessage,
    ) : FakeWmiEvent

    public data object Closed : FakeWmiEvent
}
