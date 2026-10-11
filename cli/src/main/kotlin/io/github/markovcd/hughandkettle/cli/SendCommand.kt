package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.OtherMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.hexToBytes
import io.github.markovcd.hughandkettle.wmi.Target
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Sends one message to a WMI-1 and prints what went out. `--keep-open N` holds the link N seconds
 * afterwards, so a banner or a reply can be read on stderr with `--verbose`.
 */
public object SendCommand : Command {
    override val name: String = "send"
    override val usage: String =
        """
        hk send <host[:port]> cc <controller> <value> [--channel 1..16] [--keep-open N] [--verbose]
        hk send <host[:port]> pc <program> [--channel 1..16] [--keep-open N] [--verbose]
        hk send <host[:port]> raw <hex bytes> [--keep-open N] [--verbose]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(Target::parse)
        val message = target?.let { message(args, err) }
        val keepOpen =
            args.int("keep-open", 0).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        if (target == null || message == null || keepOpen < 0) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val ended = CountDownLatch(1)
        val lostBecause = AtomicReference<String?>()
        val link =
            WmiLink
                .open(
                    target.host,
                    target.port,
                    onBytes = { if (args.flag("verbose")) err.println(Received.line(it)) },
                    onClosed = { reason ->
                        lostBecause.set(reason)
                        ended.countDown()
                    },
                ).getOrElse {
                    err.println("cannot reach ${target.host}:${target.port}: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        link.use {
            it.send(message).getOrElse { failure ->
                err.println("send failed: ${failure.message}")
                return ExitCodes.NO_LINK
            }
            out.println(
                Json.line("host" to target.host, "port" to target.port, *MidiJson.fields(message).toTypedArray()),
            )
            ended.await(keepOpen.toLong(), TimeUnit.SECONDS)
        }
        lostBecause.get()?.let { err.println("link lost: $it") }
        return ExitCodes.OK
    }

    private fun message(
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
        val kind = args.positional.getOrNull(1)
        val numbers = args.positional.drop(2).map { it.toIntOrNull() }
        return when {
            kind == "cc" && numbers.size == 2 && numbers.all { it in 0..127 } -> {
                ControlChange(MidiChannel(channel), numbers[0]!!, numbers[1]!!)
            }

            kind == "pc" && numbers.size == 1 && numbers[0] in 0..127 -> {
                ProgramChange(MidiChannel(channel), numbers[0]!!)
            }

            kind == "raw" -> {
                args.positional
                    .drop(2)
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
