package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.toHex

/** How `--verbose` spells a chunk the WMI-1 sent: hex, then the printable bytes, so a banner reads as text. */
public object Received {
    public fun line(bytes: ByteArray): String {
        val text =
            bytes.map { it.toInt() and 0xFF }.joinToString("") {
                if (it in
                    0x20..0x7E
                ) {
                    it.toChar().toString()
                } else {
                    "."
                }
            }
        return "received ${bytes.toHex()} |$text|"
    }
}
