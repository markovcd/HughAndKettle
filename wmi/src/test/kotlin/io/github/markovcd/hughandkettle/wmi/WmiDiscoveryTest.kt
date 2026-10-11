package io.github.markovcd.hughandkettle.wmi

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
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

    @Test
    fun `Every packet heard is handed over raw, one too short to be a broadcast included`() {
        val port = freeUdpPort()
        val sizes = LinkedBlockingQueue<Int>()
        FakeWmi(onEvent = {}).use { fake ->
            DatagramSocket().use { junk ->
                val to = InetSocketAddress(InetAddress.getLoopbackAddress(), port)
                val sender =
                    thread(isDaemon = true) {
                        // Repeated, since a datagram sent before discovery is bound is lost.
                        while (!junk.isClosed) {
                            runCatching { junk.send(DatagramPacket(ByteArray(10), 10, to)) }
                            runCatching { Thread.sleep(20) }.onFailure { return@thread }
                        }
                    }
                fake.broadcast(InetAddress.getLoopbackAddress(), port, interval = 100.milliseconds)
                // Stops at the first broadcast heard after a short packet; 5 s caps a broken sender.
                WmiDiscovery(port)
                    .listen(5.seconds, onPacket = { _, bytes -> sizes.put(bytes.size) }) { 10 !in sizes }
                    .getOrThrow()
                sender.interrupt()
            }
        }
        assertTrue(10 in sizes && 110 in sizes, "packet sizes heard: $sizes")
    }

    private fun freeUdpPort(): Int {
        val socket = DatagramSocket(0)
        val port = socket.localPort
        socket.close()
        check(port > 0) { "no ephemeral UDP port was handed out" }
        return port
    }
}
