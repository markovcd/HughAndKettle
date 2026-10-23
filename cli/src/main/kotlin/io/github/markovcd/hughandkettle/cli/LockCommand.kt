package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import java.io.PrintWriter
import kotlin.time.TimeSource

/**
 * Holds the amp's delay time across preset recalls, as the app's lock does, until `--seconds` pass or,
 * without it, until stdin closes. The time held is the one given, else the first the amp reports, and
 * moves with any change of it made on the amp. Prints a line each time it moves and each time a recalled
 * preset is given it back.
 */
public object LockCommand : Command {
    override val name: String = "lock"
    override val usage: String = "hk lock <host[:port]> <amp-id> [<0..127 | 520ms>] [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val seconds = args.optionalInt("seconds").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size !in 2..3 || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val time = amp.controls.firstOrNull(Control::timed)
        val scale = time?.scale
        if (time == null || scale == null) {
            err.println("${amp.id} has no delay time to hold")
            return ExitCodes.BAD_ARGUMENTS
        }
        val given =
            args.positional.getOrNull(2)?.let { text ->
                ControlValue.parse(time, text).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
            }
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.endAfter(seconds)
        val started = TimeSource.Monotonic.markNow()

        fun print(
            what: String,
            value: Int,
        ) = out.println(
            Json.line(
                "ms" to started.elapsedNow().inWholeMilliseconds,
                what to value,
                "delay_ms" to scale.amount(value),
            ),
        )
        var answers = RecallAnswers(lock = TempoLock(time, given?.value))
        answers.lock?.value?.let { print("held", it) }
        link.use {
            while (true) {
                val message = (link.next() as? QueuedLink.Next.Heard)?.message ?: break
                val before = answers.lock?.value
                val answered = answers.heard(message)
                answers = answered.answers
                answers.lock
                    ?.value
                    ?.takeIf { it != before }
                    ?.let { value -> print("held", value) }
                val restore = answered.restored ?: continue
                it.send(restore).getOrElse { failure ->
                    err.println("send failed: ${failure.message}")
                    return ExitCodes.NO_LINK
                }
                print("restored", restore.value)
            }
        }
        val reason = link.lost ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }
}
