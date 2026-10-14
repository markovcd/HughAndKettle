package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.wmi.WmiAddress

/**
 * How the last WMI-1 used is written to the phone: one text value, `<version> <host:port>`. A value
 * from a newer version, or one that does not read, is no last box rather than a failure.
 */
object LastWmi {
    const val VERSION: Int = 1

    fun write(address: WmiAddress): String = "$VERSION $address"

    fun read(text: String?): WmiAddress? {
        if (text == null || ' ' !in text) return null
        if (text.substringBefore(' ').toIntOrNull() != VERSION) return null
        return WmiAddress.parse(text.substringAfter(' '))
    }
}
