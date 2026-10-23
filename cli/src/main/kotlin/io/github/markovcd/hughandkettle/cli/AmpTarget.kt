package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.WmiAddress

/** Where a command reaches the amp: a WMI-1 by `<host[:port]>`, or a MIDI cable by `cable:<name>`. */
internal sealed interface AmpTarget {
    data class Wmi(
        val address: WmiAddress,
    ) : AmpTarget {
        override fun toString(): String = "$address"
    }

    /** The first cable whose name or description holds [part], ignoring case. */
    data class Cable(
        val part: String,
    ) : AmpTarget {
        override fun toString(): String = "$PREFIX$part"
    }

    companion object {
        /** How [AmpTarget] is spelled in a command's usage. */
        const val USAGE = "<host[:port] | cable:name>"

        private const val PREFIX = "cable:"

        /** The target [text] names; null when it names none. */
        fun parse(text: String): AmpTarget? =
            if (text.startsWith(PREFIX)) {
                text.removePrefix(PREFIX).takeIf { it.isNotBlank() }?.let(::Cable)
            } else {
                WmiAddress.parse(text)?.let(::Wmi)
            }
    }
}
