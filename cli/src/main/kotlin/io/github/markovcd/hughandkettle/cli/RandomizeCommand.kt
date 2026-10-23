package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.amp.EffectRoll
import io.github.markovcd.hughandkettle.midi.ControlChange
import java.io.PrintWriter
import kotlin.random.Random

/**
 * Randomizes the effects that are on, as the app's dice does: listens for `--seconds` to learn which are on,
 * then gives the controls behind them new values. `--keep-time` keeps the delay time, as a tempo lock does;
 * `--seed N` rolls the same values every time. Prints the values sent and the ones that put them back.
 */
public object RandomizeCommand : Command {
    override val name: String = "randomize"
    override val usage: String = "hk randomize ${AmpTarget.USAGE} <amp-id> [--seconds N] [--keep-time] [--seed N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        val seconds = args.int("seconds", DEFAULT_SECONDS).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val seed = args.optionalInt("seed").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size != 2 || seconds < 1) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val kept = if (args.flag("keep-time")) amp.controls.firstOrNull { it.timed } else null
        var state = AmpState(amp)
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.use {
            if (!it.hearFor(seconds * 1000L) { message -> state = state.after(message) }) {
                err.println("link lost: ${it.lost}")
                return ExitCodes.NO_LINK
            }
            val roll = EffectRoll.of(state, seed?.let(::Random) ?: Random.Default, kept)
            if (roll == null) {
                err.println("no effect was heard switched on in $seconds s")
                return ExitCodes.NOTHING_FOUND
            }
            it.sendAll(roll.changes).orExit(err, ExitCodes.NO_LINK) { code -> return code }
            out.println(
                Json.line(
                    "amp" to amp.id,
                    "effects" to roll.effects.map { effect -> effect.id },
                    "sent" to values(amp, roll.changes),
                    "undo" to values(amp, roll.undo),
                ),
            )
        }
        return ExitCodes.OK
    }

    /** Each change as its control's id and value, the way `hk state` prints values. */
    private fun values(
        amp: AmpModel,
        changes: List<ControlChange>,
    ): Map<String, Int> =
        changes.associate { change ->
            amp.controls.first { it.controller == change.controller }.id to
                change.value
        }

    private const val DEFAULT_SECONDS = 2
}
