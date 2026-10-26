package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.TempoLock
import java.io.PrintWriter
import kotlin.time.TimeSource

/**
 * Holds the amp's delay time across preset recalls, as the app's lock does, until `--seconds` pass or,
 * without it, until stdin closes. The time held is the one given, or a `--note` (a quarter unless given) at
 * `--tempo`, as the app's lock holds the song's tempo, else the first the amp reports, and
 * moves with any change of it made on the amp. Prints a line each time it moves and each time a recalled
 * preset is given it back.
 */
public object LockCommand : Command {
    override val name: String = "lock"
    override val usage: String =
        "hk lock ${AmpTarget.USAGE} <amp-id> [<0..127 | 520ms> | --tempo BPM [--note 1/8d]] [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
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
        val note = NoteArgument.parse(args).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val tempo = args.optionalInt("tempo").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (tempo != null && (tempo !in Song.TEMPOS || args.positional.size == 3)) {
            err.println("--tempo is ${Song.TEMPOS.first}..${Song.TEMPOS.last} BPM, given instead of a time")
            return ExitCodes.BAD_ARGUMENTS
        }
        val given =
            tempo?.let { time.set(scale.value(note.ms(it))) }
                ?: args.positional.getOrNull(2)?.let { text ->
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
                it.sendAll(listOf(restore)).orExit(err, ExitCodes.NO_LINK) { code -> return code }
                print("restored", restore.value)
            }
        }
        val reason = link.lost ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }
}
