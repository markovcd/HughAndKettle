package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.LinkKeeper
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import kotlin.time.TimeSource

/**
 * Opens a link and prints every message the amp sends over the WMI-1 or the cable, with the milliseconds since
 * the link opened, until `--seconds` pass or, without it, until stdin closes. A link the WMI-1 drops ends it
 * with the no-link code; with `--reconnect` the link to a WMI-1 is kept up the way the app keeps it, and every
 * change of its state is printed too.
 */
public object ListenCommand : Command {
    override val name: String = "listen"
    override val usage: String = "hk listen ${AmpTarget.USAGE} [--seconds N] [--reconnect] [--verbose]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        val seconds = args.optionalInt("seconds").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val reconnect = args.flag("reconnect")
        if (target == null || (seconds != null && seconds < 1) || (reconnect && target !is AmpTarget.Wmi)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        if (reconnect && target is AmpTarget.Wmi) return kept(target.address, seconds, out)
        val opened = TimeSource.Monotonic.markNow()
        val link =
            QueuedLink
                .open(target, onBytes = { if (args.flag("verbose")) err.println(Received.line(it)) })
                .orExit(err, ExitCodes.NO_LINK) { return it }
        link.endAfter(seconds)
        link.use {
            while (true) {
                val message = (it.next() as? QueuedLink.Next.Heard)?.message ?: break
                val ms = opened.elapsedNow().inWholeMilliseconds
                out.println(Json.line("ms" to ms, *MidiJson.fields(message).toTypedArray()))
            }
        }
        val reason = link.lost ?: return ExitCodes.OK
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
        RunLength.await(CountDownLatch(1), seconds)
        keeper.close()
        // Bounded by the link's connect timeout: an open still in flight is waited out and closed.
        keeper.awaitClosed(WmiDefaults.ConnectTimeout * 2)
        return ExitCodes.OK
    }
}
