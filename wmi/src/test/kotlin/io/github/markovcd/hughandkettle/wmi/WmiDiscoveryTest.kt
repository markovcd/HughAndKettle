package io.github.markovcd.hughandkettle.wmi

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class WmiDiscoveryTest {
    @Test
    fun `A broadcasting WMI-1 is found and listening stops as soon as it is`() {
        val port = freeUdpPort()
        FakeWmi(onEvent = {}).use { fake ->
            fake.broadcast(InetAddress.getLoopbackAddress(), port, interval = 100.milliseconds)
            // Capped at 5 s so a broken broadcaster fails rather than hangs; the first packet ends it.
            val found = WmiDiscovery(port).listen(5.seconds) { false }.getOrThrow()
            assertEquals(listOf("WMI-1_FAKE" to fake.port), found.map { it.deviceId to it.midiPort })
        }
    }

    private fun freeUdpPort(): Int {
        val socket = java.net.DatagramSocket(0)
        val port = socket.localPort
        socket.close()
        check(port > 0) { "no ephemeral UDP port was handed out" }
        return port
    }
}
