package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.AmpLink
import io.github.markovcd.hughandkettle.midi.JavaCableLink
import io.github.markovcd.hughandkettle.midi.JavaCables
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * A link to the amp, over a WMI-1 or a MIDI cable, whose messages wait in a queue for a command's own loop to
 * take, so the loop answers them on its thread. The run is over when the link is lost, or when [endAfter] says.
 */
internal class QueuedLink private constructor(
    private val link: AmpLink,
    private val queue: LinkedBlockingQueue<Any>,
    private val lostBecause: AtomicReference<String?>,
    /** Where the link goes, as the fields a command prints: `host` and `port`, or `cable`. */
    val where: List<Pair<String, Any?>>,
) : AutoCloseable {
    /** What [next] finds. */
    sealed interface Next {
        data class Heard(
            val message: MidiMessage,
        ) : Next

        /** Nothing heard in the time given. */
        data object Quiet : Next

        data object Over : Next
    }

    /** Why the link was lost, or null while it is up or after the run ended on time. */
    val lost: String? get() = lostBecause.get()

    /** The next message heard, waiting at most [wait] when given; [Next.Over] once the run is over. */
    fun next(wait: Long? = null): Next {
        val item =
            if (wait == null) {
                queue.take()
            } else {
                queue.poll(wait.coerceAtLeast(0), TimeUnit.MILLISECONDS) ?: return Next.Quiet
            }
        return if (item is MidiMessage) Next.Heard(item) else Next.Over
    }

    fun send(message: MidiMessage): Result<Unit> = link.send(message)

    /** Sends [messages] in order, each handed to [onSent] once it is out, up to the first that fails. */
    fun sendAll(
        messages: List<MidiMessage>,
        onSent: (MidiMessage) -> Unit = {},
    ): Result<Unit> {
        for (message in messages) {
            link.send(message).onFailure { return Result.failure(IOException("send failed: ${it.message}", it)) }
            onSent(message)
        }
        return Result.success(Unit)
    }

    /** Ends the run when [seconds] pass or, without them, when stdin closes. */
    fun endAfter(seconds: Int?) {
        thread(name = "run-until", isDaemon = true) {
            RunLength.await(CountDownLatch(1), seconds)
            queue.put(Ended)
        }
    }

    /**
     * Takes every message heard for [millis] milliseconds, handing each to [onHeard]; false when the link is
     * lost first.
     */
    fun hearFor(
        millis: Long,
        onHeard: (MidiMessage) -> Unit,
    ): Boolean {
        val deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(millis)
        while (true) {
            val left = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
            if (left <= 0) return true
            when (val next = next(left)) {
                is Next.Heard -> onHeard(next.message)
                Next.Quiet -> return true
                Next.Over -> return false
            }
        }
    }

    override fun close() = link.close()

    /** What the queue holds once the run is over. */
    private object Ended

    companion object {
        /** A link to [target], or why there is none; over a WMI-1 its raw bytes go to [onBytes] as they come. */
        fun open(
            target: AmpTarget,
            onBytes: (ByteArray) -> Unit = {},
        ): Result<QueuedLink> {
            val queue = LinkedBlockingQueue<Any>()
            val lostBecause = AtomicReference<String?>()
            return when (target) {
                is AmpTarget.Wmi -> {
                    val address = target.address
                    WmiLink
                        .open(
                            address.host,
                            address.port,
                            onMessage = queue::put,
                            onBytes = onBytes,
                            onClosed = {
                                lostBecause.set(it)
                                queue.put(Ended)
                            },
                        ).fold(
                            {
                                Result.success(
                                    QueuedLink(
                                        it,
                                        queue,
                                        lostBecause,
                                        listOf(
                                            "host" to address.host,
                                            "port" to address.port,
                                        ),
                                    ),
                                )
                            },
                            { Result.failure(IOException("cannot reach $target: ${it.message}", it)) },
                        )
                }

                is AmpTarget.Cable -> {
                    val cable =
                        JavaCables.find(target.part).getOrElse {
                            return Result.failure(
                                IOException("cannot read this machine's MIDI ports: ${it.message}", it),
                            )
                        } ?: return Result.failure(
                            IOException(
                                "no MIDI cable named like '${target.part}'; `hk cable list` shows what is plugged in",
                            ),
                        )
                    JavaCableLink
                        .open(cable, onMessage = queue::put)
                        .map { QueuedLink(it, queue, lostBecause, listOf("cable" to cable.name)) }
                }
            }
        }
    }
}
