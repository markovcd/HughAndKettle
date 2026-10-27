package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Lfo
import io.github.markovcd.hughandkettle.amp.LfoShape
import io.github.markovcd.hughandkettle.amp.Section
import java.io.PrintWriter
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Sweeps one effect control, as the app's LFO does, until `--seconds` pass or, without them, until stdin
 * closes: a change at most every [Lfo.STEP], and none while the amp is heard already there, so a recalled
 * preset is swept again at once. Prints the LFO when it starts and how many changes it sent when it ends.
 */
public object LfoCommand : Command {
    override val name: String = "lfo"
    override val usage: String =
        "hk lfo ${AmpTarget.USAGE} <amp-id> <control> [--shape sine] [--hz 0.5] [--low 0] [--high 127] [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        val seconds = args.optionalInt("seconds").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val low = args.int("low", 0).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val high = args.int("high", 127).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size != 3 || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val swept = amp.controls.filter { it.section == Section.Fx && it.kind != ControlKind.Switch }
        val control = swept.firstOrNull { it.id == args.positional[2] }
        if (control == null) {
            err.println("an LFO sweeps one of ${swept.joinToString(", ") { it.id }}, got '${args.positional[2]}'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val shapeId = args.value("shape") ?: LfoShape.Sine.id
        val shape = LfoShape.byId(shapeId)
        if (shape == null) {
            err.println("--shape is one of ${LfoShape.entries.joinToString(", ") { it.id }}, got '$shapeId'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val hz = args.value("hz")?.let { it.toDoubleOrNull() ?: Double.NaN } ?: Lfo.DEFAULT_HZ
        if (hz !in Lfo.SLOWEST_HZ..Lfo.FASTEST_HZ || low !in 0..127 || high !in 0..127) {
            err.println("--hz is ${Lfo.SLOWEST_HZ}..${Lfo.FASTEST_HZ}, --low and --high 0..127")
            return ExitCodes.BAD_ARGUMENTS
        }
        val lfo = Lfo(control, shape, hz, low, high)
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.endAfter(seconds)
        out.println(
            Json.line(
                "amp" to amp.id,
                "control" to control.id,
                "shape" to shape.id,
                "hz" to hz,
                "low" to low,
                "high" to high,
            ),
        )
        var state = AmpState(amp)
        var sent = 0
        val started = TimeSource.Monotonic.markNow()
        var due = Duration.ZERO
        link.use {
            while (true) {
                val wait = (due - started.elapsedNow()).inWholeMilliseconds
                if (wait > 0) {
                    when (val next = it.next(wait)) {
                        is QueuedLink.Next.Heard -> {
                            state = state.after(next.message)
                            continue
                        }

                        QueuedLink.Next.Quiet -> {}

                        QueuedLink.Next.Over -> {
                            break
                        }
                    }
                }
                val value = lfo.value(started.elapsedNow())
                if (value != state.values[control.id]) {
                    val change = control.set(value)
                    it.send(change).orExit(err, ExitCodes.NO_LINK) { code -> return code }
                    state = state.after(change)
                    sent++
                }
                due += Lfo.STEP
            }
        }
        out.println(Json.line("sent" to sent))
        val reason = link.lost ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }
}
