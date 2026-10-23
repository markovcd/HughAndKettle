package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.amp.VolumeTrim
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import java.io.PrintWriter
import kotlin.time.TimeSource

/**
 * Adds a trim to the volume of every preset the amp recalls and every channel it switches to, as the
 * app's volume trim does, until `--seconds` pass or, without it, until stdin closes. `--recall N` first
 * recalls preset N behind the mute, as the app does. Prints each program change and value block heard and
 * each change sent, with the milliseconds since the link opened, so the gap the stored volume plays for
 * can be read off.
 */
public object TrimCommand : Command {
    override val name: String = "trim"
    override val usage: String = "hk trim <host[:port]> <amp-id> <-127..127> [--recall N] [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val amount = args.positional.getOrNull(2)?.toIntOrNull()
        val seconds = args.optionalInt("seconds").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val preset = args.optionalInt("recall").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || args.positional.size != 3 || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        if (amount == null || amount !in -127..127) {
            err.println("a trim takes -127..127, got '${args.positional[2]}'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val volume = amp.control("volume")
        val mute = amp.control("mute")
        if (volume == null || mute == null) {
            err.println("${amp.id} has no volume and mute to trim with")
            return ExitCodes.BAD_ARGUMENTS
        }
        val recall =
            preset?.let {
                if (it !in 1..amp.presetCount) {
                    err.println("${amp.name} has presets 1..${amp.presetCount}, got $it")
                    return ExitCodes.BAD_ARGUMENTS
                }
                amp.recall(it)
            }
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.endAfter(seconds)
        val started = TimeSource.Monotonic.markNow()

        fun print(vararg fields: Pair<String, Any?>) =
            out.println(
                Json.line("ms" to started.elapsedNow().inWholeMilliseconds, *fields),
            )

        var answers = RecallAnswers(trim = VolumeTrim.of(volume, amount))
        link.use {
            fun send(messages: List<MidiMessage>): Boolean {
                for (message in messages) {
                    it.send(message).getOrElse { failure ->
                        err.println("send failed: ${failure.message}")
                        return false
                    }
                    print("direction" to "sent", *MidiJson.fields(message).toTypedArray())
                }
                return true
            }
            var muteDeadline: Long? = null
            if (recall != null) {
                val (behind, messages) = answers.recall(recall, mute, muted = false)
                answers = behind
                if (!send(messages)) return ExitCodes.NO_LINK
                muteDeadline = started.elapsedNow().inWholeMilliseconds + VolumeTrim.muteLimit.inWholeMilliseconds
            }
            while (true) {
                val wait = muteDeadline?.let { (it - started.elapsedNow().inWholeMilliseconds).coerceAtLeast(0) }
                val next = link.next(wait)
                if (next == QueuedLink.Next.Quiet) {
                    val (unmuted, lift) = answers.unmuted()
                    answers = unmuted
                    muteDeadline = null
                    print("muted_too_long_ms" to VolumeTrim.muteLimit.inWholeMilliseconds)
                    if (!send(listOfNotNull(lift))) return ExitCodes.NO_LINK
                    continue
                }
                val message = (next as? QueuedLink.Next.Heard)?.message ?: break
                describe(message, volume.controller)?.let { print(*it) }
                val answered = answers.heard(message)
                answers = answered.answers
                if (!answers.muting) muteDeadline = null
                if (!send(answered.replies)) return ExitCodes.NO_LINK
            }
        }
        val reason = link.lost ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }

    /** The fields printed for [message] heard: a program change, or a value block's preset and volume. */
    private fun describe(
        message: MidiMessage,
        volume: Int,
    ): Array<Pair<String, Any?>>? =
        when (message) {
            is ProgramChange -> {
                arrayOf("direction" to "heard", "type" to "program_change", "preset" to message.program + 1)
            }

            is SystemExclusive -> {
                ValueBlock.parse(message)?.let {
                    arrayOf(
                        "direction" to "heard",
                        "type" to "value_block",
                        "preset" to it.preset,
                        "channel" to it.channel,
                        "volume" to it.values[volume]?.div(2),
                    )
                }
            }

            else -> {
                null
            }
        }
}
