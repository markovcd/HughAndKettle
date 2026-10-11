package io.github.markovcd.hughandkettle.wmi

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * What the WMI-1 does out of the box, as far as is known.
 *
 * The box carries a Roving Networks RN-171 WiFi module (its FCC ID is printed on the label), whose
 * firmware ships a transparent TCP bridge to the serial port on 2000 and a status broadcast to UDP
 * 55555 every seven seconds. docs/wmi-1.md says what has been confirmed on hardware and what has not.
 */
public object WmiDefaults {
    public const val MIDI_PORT: Int = 2000
    public const val BROADCAST_PORT: Int = 55555
    public val BroadcastInterval: Duration = 7.seconds
    public val ConnectTimeout: Duration = 5.seconds
}
