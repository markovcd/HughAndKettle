package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.SystemRealtime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What the WMI-1 does out of the box, as read off one on 2026-10-13 (WiFly firmware 4.41).
 *
 * Its RN-171 module serves the MIDI bridge on TCP 1470 and broadcasts its status to UDP 48437 every
 * two seconds. It drops a client that has sent nothing for about 30 seconds, and holds what the amp
 * says until the client sends something, so a link [pings][Ping] the box every [PingInterval].
 * docs/wmi-1.md has the evidence.
 */
public object WmiDefaults {
    public const val MIDI_PORT: Int = 1470

    /** The box's own address on the network it makes in access point mode. */
    public const val ACCESS_POINT_HOST: String = "192.168.1.1"

    public const val BROADCAST_PORT: Int = 48437

    /** Where the box broadcasts after a full reset with SET held at power-up: the RN-171's own default. */
    public const val FACTORY_BROADCAST_PORT: Int = 55555
    public val BroadcastPorts: List<Int> = listOf(BROADCAST_PORT, FACTORY_BROADCAST_PORT)
    public val BroadcastInterval: Duration = 2.seconds
    public val ConnectTimeout: Duration = 5.seconds
    public val PingInterval: Duration = 1.seconds

    /** An undefined real-time byte: it flushes the box, and MIDI tells every receiver to ignore it. */
    public val Ping: MidiMessage = SystemRealtime(0xFD)
}
