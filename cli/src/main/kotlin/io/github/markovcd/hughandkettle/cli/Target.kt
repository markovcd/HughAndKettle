package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.WmiDefaults

/** A `host[:port]` argument. */
public data class Target(
    val host: String,
    val port: Int,
) {
    public companion object {
        public fun parse(text: String): Target? {
            val host = text.substringBefore(':')
            if (host.isEmpty()) return null
            if (!text.contains(':')) return Target(host, WmiDefaults.MIDI_PORT)
            val port = text.substringAfter(':').toIntOrNull() ?: return null
            if (port !in 1..65535) return null
            return Target(host, port)
        }
    }
}
