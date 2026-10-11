package io.github.markovcd.hughandkettle.wmi

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/** Hears WMI-1 status broadcasts on the local network. */
public class WmiDiscovery(
    private val broadcastPort: Int = WmiDefaults.BROADCAST_PORT,
) {
    /**
     * Listens for up to [duration], handing each broadcast to [onFound] as it arrives, and returns the
     * distinct devices heard. [onFound] returns false to stop early. Fails as a value: a port the
     * machine will not open comes back as a [Result] failure.
     */
    public fun listen(
        duration: Duration,
        onFound: (WmiBroadcast) -> Boolean = { true },
    ): Result<List<WmiBroadcast>> {
        val socket =
            runCatching {
                DatagramSocket(null).apply {
                    reuseAddress = true
                    broadcast = true
                    bind(InetSocketAddress(broadcastPort))
                }
            }.getOrElse { return Result.failure(it) }
        socket.use {
            val found = LinkedHashMap<String, WmiBroadcast>()
            val buffer = ByteArray(1024)
            val started = TimeSource.Monotonic.markNow()
            while (true) {
                val left = duration - started.elapsedNow()
                if (left <= Duration.ZERO) break
                socket.soTimeout =
                    left.inWholeMilliseconds
                        .coerceIn(1, 500)
                        .toInt()
                        .coerceAtLeast(1)
                val packet = DatagramPacket(buffer, buffer.size)
                try {
                    socket.receive(packet)
                } catch (_: SocketTimeoutException) {
                    continue
                }
                val broadcast = WmiBroadcast.parse(buffer, packet.length, packet.address) ?: continue
                found["${broadcast.address.hostAddress}:${broadcast.midiPort}"] = broadcast
                if (!onFound(broadcast)) break
            }
            return Result.success(found.values.toList())
        }
    }

    public companion object {
        /** Long enough for one broadcast at the default interval, plus slack for a dropped packet. */
        public val DefaultDuration: Duration = WmiDefaults.BroadcastInterval * 2 + 500.milliseconds
    }
}
