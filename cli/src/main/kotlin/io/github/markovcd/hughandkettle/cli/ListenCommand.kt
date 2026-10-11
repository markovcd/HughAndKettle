package io.github.markovcd.hughandkettle.cli

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
 * with the no-link code.
 */
public object ListenCommand : Command {
    override val name: String = "listen"
    override val usage: String = "hk listen <host[:port]> [--seconds N] [--verbose]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(Target::parse)
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
        link.use {
            if (seconds != null) {
                ended.await(seconds.toLong(), TimeUnit.SECONDS)
            } else {
                thread(name = "listen-stdin", isDaemon = true) {
                    while (System.`in`.read() >= 0) {
                        // Input is ignored; closing it is the signal to stop.
                    }
                    ended.countDown()
                }
                ended.await()
            }
        }
        val reason = lostBecause.get() ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }
}
