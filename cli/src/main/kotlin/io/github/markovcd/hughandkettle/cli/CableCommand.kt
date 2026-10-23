package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.JavaCable
import io.github.markovcd.hughandkettle.midi.JavaCableLink
import io.github.markovcd.hughandkettle.midi.JavaCables
import io.github.markovcd.hughandkettle.midi.MidiStreamParser
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import kotlin.time.TimeSource

/**
 * Drives the amp over a USB MIDI cable plugged into this machine, with no WMI-1: lists the cables,
 * sends one message, or prints every message the amp sends until `--seconds` pass or stdin closes. A
 * cable is named by any part of its name; every other command that drives the amp takes it as `cable:<name>`.
 */
public object CableCommand : Command {
    override val name: String = "cable"
    override val usage: String =
        """
        hk cable list
        hk cable send <name> cc <controller> <value> [--channel 1..16]
        hk cable send <name> pc <program> [--channel 1..16]
        hk cable send <name> raw <hex bytes>
        hk cable listen <name> [--seconds N]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val words = args.positional
        if (words == listOf("list")) return list(out, err)
        val part = words.getOrNull(1)
        val seconds = args.optionalInt("seconds").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val wanted =
            when (words.firstOrNull()) {
                "send" -> part != null
                "listen" -> part != null && words.size == 2 && (seconds == null || seconds >= 1)
                else -> false
            }
        if (!wanted || part == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val cable =
            JavaCables.find(part).getOrElse {
                err.println("cannot read this machine's MIDI ports: ${it.message}")
                return ExitCodes.NO_LINK
            }
        if (cable == null) {
            err.println("no MIDI cable named like '$part'; `hk cable list` shows what is plugged in")
            return ExitCodes.NOTHING_FOUND
        }
        return if (words[0] == "send") send(cable, words.drop(2), args, out, err) else listen(cable, seconds, out, err)
    }

    private fun list(
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val cables =
            JavaCables.all().getOrElse {
                err.println("cannot read this machine's MIDI ports: ${it.message}")
                return ExitCodes.NO_LINK
            }
        cables.forEach {
            out.println(
                Json.line(
                    "name" to it.name,
                    "description" to it.description,
                    "to_amp" to (it.toAmp != null),
                    "from_amp" to (it.fromAmp != null),
                ),
            )
        }
        return if (cables.isEmpty()) ExitCodes.NOTHING_FOUND else ExitCodes.OK
    }

    private fun send(
        cable: JavaCable,
        words: List<String>,
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val message = SendCommand.message(words, args, err)
        if (message == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        if (cable.toAmp == null) {
            err.println("${cable.name} takes no messages to send")
            return ExitCodes.NO_LINK
        }
        // A raw message may hold several; each goes out whole, as the parser cuts them.
        val messages = MidiStreamParser().feed(message.encode())
        JavaCableLink
            .open(cable)
            .mapCatching { link -> link.use { it.write(message.encode()).getOrThrow() } }
            .getOrElse {
                err.println("cannot send on ${cable.name}: ${it.message}")
                return ExitCodes.NO_LINK
            }
        messages.forEach { out.println(Json.line("cable" to cable.name, *MidiJson.fields(it).toTypedArray())) }
        return ExitCodes.OK
    }

    private fun listen(
        cable: JavaCable,
        seconds: Int?,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val device = cable.fromAmp
        if (device == null) {
            err.println("${cable.name} hears nothing to listen to")
            return ExitCodes.NO_LINK
        }
        val opened = TimeSource.Monotonic.markNow()
        val link =
            JavaCableLink
                .open(JavaCable(cable.name, cable.description, toAmp = null, fromAmp = device)) { message ->
                    val ms = opened.elapsedNow().inWholeMilliseconds
                    synchronized(out) { out.println(Json.line("ms" to ms, *MidiJson.fields(message).toTypedArray())) }
                }.getOrElse {
                    err.println("cannot listen on ${cable.name}: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        link.use { RunLength.await(CountDownLatch(1), seconds) }
        return ExitCodes.OK
    }
}
