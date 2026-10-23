package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.OtherMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.hexToBytes
import java.io.PrintWriter

/**
 * Sends one message to the amp over a WMI-1 or a cable and prints what went out. `--keep-open N` holds the link N seconds
 * afterwards, so a banner or a reply can be read on stderr with `--verbose`.
 */
public object SendCommand : Command {
    override val name: String = "send"
    override val usage: String =
        """
        hk send ${AmpTarget.USAGE} cc <controller> <value> [--channel 1..16] [--keep-open N] [--verbose]
        hk send ${AmpTarget.USAGE} pc <program> [--channel 1..16] [--keep-open N] [--verbose]
        hk send ${AmpTarget.USAGE} raw <hex bytes> [--keep-open N] [--verbose]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        val message = target?.let { message(args.positional.drop(1), args, err) }
        val keepOpen = args.int("keep-open", 0).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        if (target == null || message == null || keepOpen < 0) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val link =
            QueuedLink
                .open(target, onBytes = { if (args.flag("verbose")) err.println(Received.line(it)) })
                .orExit(err, ExitCodes.NO_LINK) { return it }
        link.use {
            it.sendAll(listOf(message)).orExit(err, ExitCodes.NO_LINK) { code -> return code }
            out.println(Json.line(*it.where.toTypedArray(), *MidiJson.fields(message).toTypedArray()))
            it.hearFor(keepOpen * 1000L) {}
        }
        link.lost?.let { err.println("link lost: $it") }
        return ExitCodes.OK
    }

    /** The message [words] spell: `cc <controller> <value>`, `pc <program>` or `raw <hex>`; null for anything else. */
    internal fun message(
        words: List<String>,
        args: Options,
        err: PrintWriter,
    ): MidiMessage? {
        val channel =
            args.int("channel", 1).getOrElse {
                err.println(it.message)
                return null
            }
        if (channel !in 1..16) {
            err.println("--channel must be 1..16, got $channel")
            return null
        }
        val kind = words.firstOrNull()
        val numbers = words.drop(1).map { it.toIntOrNull() }
        return when {
            kind == "cc" && numbers.size == 2 && numbers.all { it in 0..127 } -> {
                ControlChange(MidiChannel(channel), numbers[0]!!, numbers[1]!!)
            }

            kind == "pc" && numbers.size == 1 && numbers[0] in 0..127 -> {
                ProgramChange(MidiChannel(channel), numbers[0]!!)
            }

            kind == "raw" -> {
                words
                    .drop(1)
                    .joinToString(" ")
                    .hexToBytes()
                    ?.let(::OtherMessage)
            }

            else -> {
                null
            }
        }
    }
}
