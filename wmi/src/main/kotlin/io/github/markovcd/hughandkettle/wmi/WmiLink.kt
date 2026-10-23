package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.AmpLink
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.MidiStreamParser
import java.io.IOException
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.time.Duration

/**
 * An open TCP link to a WMI-1: MIDI bytes out, MIDI messages in.
 *
 * Every message goes out whole with its status byte, so the module can never see the three-byte
 * command-mode escape its firmware listens for. Incoming bytes are handed raw to [onBytes], then
 * parsed and handed to [onMessage], both on a reader thread; [onClosed] fires once, when the far end
 * goes away or [close] is called. While open, the link sends [WmiDefaults.Ping] every [pingInterval]:
 * the box forwards the amp's messages only after the client sends, and drops a silent client. The
 * amp echoes the ping back, and the echo is not handed on.
 */
public class WmiLink private constructor(
    private val socket: Socket,
    private val onMessage: (MidiMessage) -> Unit,
    private val onBytes: (ByteArray) -> Unit,
    private val onClosed: (reason: String?) -> Unit,
    private val pingInterval: Duration,
) : AmpLink {
    private val output: OutputStream = socket.getOutputStream()
    private var closed = false

    public val remote: InetSocketAddress get() = socket.remoteSocketAddress as InetSocketAddress

    private val reader =
        thread(name = "wmi-link-reader", isDaemon = true, start = false) {
            val parser = MidiStreamParser()
            val buffer = ByteArray(512)
            val reason =
                try {
                    val input = socket.getInputStream()
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        val chunk = buffer.copyOf(read)
                        onBytes(chunk)
                        parser.feed(chunk).filter { it != WmiDefaults.Ping }.forEach(onMessage)
                    }
                    "closed by the WMI-1"
                } catch (e: IOException) {
                    if (closed) null else e.message ?: "link lost"
                }
            finish(reason)
        }

    private val pinger =
        thread(name = "wmi-link-ping", isDaemon = true, start = false) {
            while (!socket.isClosed) {
                try {
                    Thread.sleep(pingInterval.inWholeMilliseconds)
                } catch (_: InterruptedException) {
                    break
                }
                // A failed write means the link is going; the reader reports why.
                if (send(WmiDefaults.Ping).isFailure) break
            }
        }

    override fun send(message: MidiMessage): Result<Unit> =
        runCatching {
            synchronized(output) {
                output.write(message.encode())
                output.flush()
            }
        }

    override fun close() {
        closed = true
        socket.close()
        pinger.interrupt()
    }

    @Synchronized
    private fun finish(reason: String?) {
        socket.close()
        onClosed(reason)
    }

    public companion object {
        /** Connects; a host that does not answer is a failure value, not an exception. */
        public fun open(
            host: String,
            port: Int = WmiDefaults.MIDI_PORT,
            timeout: Duration = WmiDefaults.ConnectTimeout,
            pingInterval: Duration = WmiDefaults.PingInterval,
            onMessage: (MidiMessage) -> Unit = {},
            onBytes: (ByteArray) -> Unit = {},
            onClosed: (reason: String?) -> Unit = {},
        ): Result<WmiLink> =
            runCatching {
                val socket = Socket()
                socket.tcpNoDelay = true
                socket.connect(InetSocketAddress(host, port), timeout.inWholeMilliseconds.toInt())
                WmiLink(socket, onMessage, onBytes, onClosed, pingInterval).also {
                    it.reader.start()
                    it.pinger.start()
                }
            }
    }
}
