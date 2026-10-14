package io.github.markovcd.hughandkettle.wmi

import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeWmiTest {
    /** What a client reads once the fake is closed: -1 for a dropped link, else a byte or a timeout. */
    private fun readAfterClose(): String {
        val fake = FakeWmi(onEvent = {})
        Socket().use { client ->
            client.connect(InetSocketAddress("127.0.0.1", fake.port))
            fake.close()
            // A dropped link reads at once; a second is far longer than loopback needs to show it.
            client.soTimeout = 1000
            return try {
                "${client.getInputStream().read()}"
            } catch (_: SocketException) {
                "-1"
            } catch (_: SocketTimeoutException) {
                "still open"
            }
        }
    }

    @Test
    fun `A fake closed just as a client connects drops that client too`() {
        // The race is between the fake's acceptor taking the client in and the close; enough tries hit it.
        val reads = List(50) { readAfterClose() }.groupingBy { it }.eachCount()
        assertEquals(mapOf("-1" to 50), reads)
    }
}
