package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.amp.EffectRoll
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

/**
 * Randomizes the effects that are on, as the app's dice does: listens for `--seconds` to learn which are on,
 * then gives the controls behind them new values. `--keep-time` keeps the delay time, as a tempo lock does;
 * `--seed N` rolls the same values every time. Prints the values sent and the ones that put them back.
 */
public object RandomizeCommand : Command {
    override val name: String = "randomize"
    override val usage: String = "hk randomize <host[:port]> <amp-id> [--seconds N] [--keep-time] [--seed N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val seconds = args.int("seconds", DEFAULT_SECONDS).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val seed = args.optionalInt("seed").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size != 2 || seconds < 1) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val kept = if (args.flag("keep-time")) amp.controls.firstOrNull { it.timed } else null
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
        link.use {
            ended.await(seconds.toLong(), TimeUnit.SECONDS)
            lostBecause.get()?.let { reason ->
                err.println("link lost: $reason")
                return ExitCodes.NO_LINK
            }
            val roll = EffectRoll.of(state.get(), seed?.let(::Random) ?: Random.Default, kept)
            if (roll == null) {
                err.println("no effect was heard switched on in $seconds s")
                return ExitCodes.NOTHING_FOUND
            }
            for (change in roll.changes) {
                it.send(change).getOrElse { failure ->
                    err.println("send failed: ${failure.message}")
                    return ExitCodes.NO_LINK
                }
            }
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
