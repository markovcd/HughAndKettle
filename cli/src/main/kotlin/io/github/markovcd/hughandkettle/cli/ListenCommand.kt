package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.LinkKeeper
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
import kotlin.time.TimeSource

/**
 * Opens a link and prints every message the WMI-1 sends, with the milliseconds since the link
 * opened, until `--seconds` pass or, without it, until stdin closes. A link the WMI-1 drops ends it
 * with the no-link code; with `--reconnect` the link is kept up the way the app keeps it, and every
 * change of its state is printed too.
 */
public object ListenCommand : Command {
    override val name: String = "listen"
    override val usage: String = "hk listen <host[:port]> [--seconds N] [--reconnect] [--verbose]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val seconds =
            args.value("seconds")?.let {
                args.int("seconds", 0).getOrElse { failure ->
                    err.println(failure.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            }
        if (target == null || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        if (args.flag("reconnect")) return kept(target, seconds, out)
        val ended = CountDownLatch(1)
        val lostBecause = AtomicReference<String?>()
        val opened = TimeSource.Monotonic.markNow()
        val link =
            WmiLink
                .open(
                    target.host,
                    target.port,
                    onMessage = { message ->
                        val ms = opened.elapsedNow().inWholeMilliseconds
                        out.println(Json.line("ms" to ms, *MidiJson.fields(message).toTypedArray()))
                    },
                    onBytes = { if (args.flag("verbose")) err.println(Received.line(it)) },
                    onClosed = { reason ->
                        lostBecause.set(reason)
                        ended.countDown()
                    },
                ).getOrElse {
                    err.println("cannot reach ${target.host}:${target.port}: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        link.use { await(ended, seconds) }
        val reason = lostBecause.get() ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }

    private fun kept(
        address: WmiAddress,
        seconds: Int?,
        out: PrintWriter,
    ): Int {
        val started = TimeSource.Monotonic.markNow()

        fun print(fields: List<Pair<String, Any?>>) =
            out.println(
                Json.line("ms" to started.elapsedNow().inWholeMilliseconds, *fields.toTypedArray()),
            )
        val keeper = LinkKeeper(onState = { print(LinkJson.fields(it)) }, onMessage = { print(MidiJson.fields(it)) })
        keeper.connect(address)
        await(CountDownLatch(1), seconds)
        keeper.close()
        // Bounded by the link's connect timeout: an open still in flight is waited out and closed.
        keeper.awaitClosed(WmiDefaults.ConnectTimeout * 2)
        return ExitCodes.OK
    }

    /** Waits for [ended], [seconds] if given, or else stdin closing. */
    private fun await(
        ended: CountDownLatch,
        seconds: Int?,
    ) {
        if (seconds != null) {
            ended.await(seconds.toLong(), TimeUnit.SECONDS)
            return
        }
        thread(name = "listen-stdin", isDaemon = true) {
            while (System.`in`.read() >= 0) {
                // Input is ignored; closing it is the signal to stop.
            }
            ended.countDown()
        }
        ended.await()
    }
}
