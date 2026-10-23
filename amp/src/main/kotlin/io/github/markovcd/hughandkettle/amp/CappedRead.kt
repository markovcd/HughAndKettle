package io.github.markovcd.hughandkettle.amp

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Reads a file brought in from outside no further than the most its format can hold. */
public object CappedRead {
    /** The bytes of [stream], or null once it runs past [most]. */
    @Throws(IOException::class)
    public fun bytes(
        stream: InputStream,
        most: Int,
    ): ByteArray? {
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        while (true) {
            val read = stream.read(buffer)
            if (read < 0) return bytes.toByteArray()
            bytes.write(buffer, 0, read)
            if (bytes.size() > most) return null
        }
    }
}
