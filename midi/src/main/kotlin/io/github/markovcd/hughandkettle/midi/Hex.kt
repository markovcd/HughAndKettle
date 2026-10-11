package io.github.markovcd.hughandkettle.midi

/** Bytes as `B0 14 7F`, the spelling every log and every test uses. */
public fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it.toInt() and 0xFF) }

/** The inverse of [toHex]; whitespace is optional. Returns null for anything that is not hex pairs. */
public fun String.hexToBytes(): ByteArray? {
    val digits = filterNot(Char::isWhitespace)
    if (digits.length % 2 != 0 || digits.any { it.digitToIntOrNull(16) == null }) return null
    return ByteArray(digits.length / 2) { digits.substring(2 * it, 2 * it + 2).toInt(16).toByte() }
}
