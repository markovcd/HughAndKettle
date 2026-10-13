package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.MidiStreamParser
import java.io.Closeable
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.time.Duration

/**
 * A stand-in for a WMI-1 with an amp behind it: accepts one TCP client at a time, reports every MIDI
 * message it receives but the link's ping, which the amp ignores, and, when asked, broadcasts a module-style status packet so discovery finds it.
 * It lets the app and the CLI be driven end to end on a machine with no amp.
 */
public class FakeWmi(
    port: Int = 0,
    private val deviceId: String = "WMI-1_FAKE",
    private val onEvent: (FakeWmiEvent) -> Unit,
) : Closeable {
    private val server = ServerSocket(port)
    private var client: Socket? = null
    private var broadcaster: Thread? = null

    public val port: Int get() = server.localPort

    private val acceptor =
        thread(name = "fake-wmi-accept", isDaemon = true) {
            while (!server.isClosed) {
                val socket =
                    try {
                        server.accept()
                    } catch (_: IOException) {
                        break
                    }
                client = socket
                serve(socket)
            }
        }

    /** Starts broadcasting a status packet every [interval] to [to], the way the real module does. */
    public fun broadcast(
        to: InetAddress,
        port: Int = WmiDefaults.BROADCAST_PORT,
        interval: Duration = WmiDefaults.BroadcastInterval,
    ) {
        broadcaster =
            thread(name = "fake-wmi-broadcast", isDaemon = true) {
                DatagramSocket().use { socket ->
                    socket.broadcast = true
                    val packet = statusPacket()
                    while (!server.isClosed) {
                        try {
                            socket.send(DatagramPacket(packet, packet.size, InetSocketAddress(to, port)))
                        } catch (_: IOException) {
                            break
                        }
                        try {
                            Thread.sleep(interval.inWholeMilliseconds)
                        } catch (_: InterruptedException) {
                            break
                        }
                    }
                }
            }
    }

    /** Sends [message] to the connected client, as the amp would on its MIDI Out. */
    public fun emit(message: MidiMessage): Boolean {
        val socket = client ?: return false
        return runCatching {
            socket.getOutputStream().apply {
                write(message.encode())
                flush()
            }
        }.isSuccess
    }

    override fun close() {
        server.close()
        client?.close()
        broadcaster?.interrupt()
    }

    private fun serve(socket: Socket) {
        onEvent(FakeWmiEvent.Opened(socket.remoteSocketAddress as InetSocketAddress))
        val parser = MidiStreamParser()
        val buffer = ByteArray(512)
        try {
            val input = socket.getInputStream()
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                parser
                    .feed(buffer.copyOf(read))
                    .filter { it != WmiDefaults.Ping }
                    .forEach { onEvent(FakeWmiEvent.Received(it)) }
            }
        } catch (_: IOException) {
            // The client went away; the acceptor waits for the next one.
        }
        socket.close()
        if (client === socket) client = null
        onEvent(FakeWmiEvent.Closed)
    }

    /** A status packet in the module's layout, carrying this fake's MIDI port and device id. */
    internal fun statusPacket(): ByteArray {
        val packet = ByteArray(110)
        packet[8] = (port shr 8).toByte()
        packet[9] = port.toByte()
        "FAKE 0.0".toByteArray(Charsets.US_ASCII).copyInto(packet, 32)
        deviceId.toByteArray(Charsets.US_ASCII).copyInto(packet, 60, 0, minOf(deviceId.length, 32))
        return packet
    }
}
