package io.github.markovcd.hughandkettle.wmi

/** Where a WMI-1 listens for a link: a host and its MIDI port. */
public data class WmiAddress(
    val host: String,
    val port: Int,
) {
    public companion object {
        /** Reads `host[:port]`, the port defaulting to the WMI-1's; null when it is not one. */
        public fun parse(text: String): WmiAddress? {
            val host = text.substringBefore(':')
            if (host.isEmpty()) return null
            if (!text.contains(':')) return WmiAddress(host, WmiDefaults.MIDI_PORT)
            val port = text.substringAfter(':').toIntOrNull() ?: return null
            if (port !in 1..65535) return null
            return WmiAddress(host, port)
        }
    }
}
