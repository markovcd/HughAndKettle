package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Listens to an amp for `--seconds` and prints what it said as one snapshot: the values of the
 * controls it reported and the preset it recalled. Nothing is asked of the amp, so a quiet amp prints
 * an empty snapshot.
 */
public object StateCommand : Command {
    override val name: String = "state"
    override val usage: String = "hk state <host[:port]> <amp-id> [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val seconds =
            args.int("seconds", DEFAULT_SECONDS).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        if (target == null || args.positional.size != 2 || seconds < 1) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(args.positional[1]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val state = AtomicReference(AmpState(amp))
        val ended = CountDownLatch(1)
        val lostBecause = AtomicReference<String?>()
        val link =
            WmiLink
                .open(
                    target.host,
                    target.port,
                    onMessage = { message -> state.updateAndGet { it.after(message) } },
                    onClosed = { reason ->
                        lostBecause.set(reason)
                        ended.countDown()
                    },
                ).getOrElse {
                    err.println("cannot reach $target: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        link.use { ended.await(seconds.toLong(), TimeUnit.SECONDS) }
        val heard = state.get()
        out.println(Json.line("amp" to amp.id, "preset" to heard.preset, "values" to heard.values))
        val reason = lostBecause.get() ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }

    private const val DEFAULT_SECONDS = 2
}
