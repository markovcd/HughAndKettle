package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread

/**
 * A link to a WMI-1 whose messages wait in a queue for a command's own loop to take, so the loop answers
 * them on its thread. The run is over when the link is lost, or when [endAfter] says.
 */
internal class QueuedLink private constructor(
    private val link: WmiLink,
    private val queue: LinkedBlockingQueue<Any>,
    private val lostBecause: AtomicReference<String?>,
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

    /** Ends the run when [seconds] pass or, without them, when stdin closes. */
    fun endAfter(seconds: Int?) {
        thread(name = "run-until", isDaemon = true) {
            RunLength.await(CountDownLatch(1), seconds)
            queue.put(Ended)
        }
    }

    override fun close() = link.close()

    /** What the queue holds once the run is over. */
    private object Ended

    companion object {
        /** A link to [target], or why there is none. */
        fun open(target: WmiAddress): Result<QueuedLink> {
            val queue = LinkedBlockingQueue<Any>()
            val lostBecause = AtomicReference<String?>()
            return WmiLink
                .open(
                    target.host,
                    target.port,
                    onMessage = queue::put,
                    onClosed = {
                        lostBecause.set(it)
                        queue.put(Ended)
                    },
                ).fold(
                    { Result.success(QueuedLink(it, queue, lostBecause)) },
                    { Result.failure(IOException("cannot reach $target: ${it.message}", it)) },
                )
        }
    }
}
