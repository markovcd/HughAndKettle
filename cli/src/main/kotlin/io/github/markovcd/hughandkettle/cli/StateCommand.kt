package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpState
import java.io.PrintWriter

/**
 * Listens to an amp for `--seconds` and prints what it said as one snapshot: the values of the
 * controls it reported and the preset it recalled. Nothing is asked of the amp, so a quiet amp prints
 * an empty snapshot.
 */
public object StateCommand : Command {
    override val name: String = "state"
    override val usage: String = "hk state ${AmpTarget.USAGE} <amp-id> [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        val seconds = args.int("seconds", DEFAULT_SECONDS).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size != 2 || seconds < 1) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        var state = AmpState(amp)
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.use { it.hearFor(seconds * 1000L) { message -> state = state.after(message) } }
        out.println(Json.line("amp" to amp.id, "preset" to state.preset, "values" to state.values))
        val reason = link.lost ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }

    private const val DEFAULT_SECONDS = 2
}
