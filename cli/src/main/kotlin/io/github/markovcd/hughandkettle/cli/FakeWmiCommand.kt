package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import java.io.PrintWriter
import java.net.InetAddress

/**
 * Runs a fake WMI-1 until `--seconds` pass or, without it, until stdin closes, printing every event
 * as a JSON line. Point the app or `hk send` at the port it prints.
 */
public object FakeWmiCommand : Command {
    override val name: String = "fake-wmi"
    override val usage: String =
        "hk fake-wmi [--port N] [--name ID] [--broadcast-to ADDRESS | --no-broadcast] [--seconds N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val port =
            args.int("port", WmiDefaults.MIDI_PORT).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val seconds =
            args.optionalInt("seconds").getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        if (seconds != null && seconds < 1) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val name = args.value("name") ?: "WMI-1_FAKE"
        val fake =
            FakeWmi(port, name) { event ->
                out.println(
                    when (event) {
                        is FakeWmiEvent.Opened -> {
                            Json.line(
                                "event" to "opened",
                                "from" to event.from.address.hostAddress,
                            )
                        }

                        is FakeWmiEvent.Received -> {
                            Json.line("event" to "midi", *MidiJson.fields(event.message).toTypedArray())
                        }

                        FakeWmiEvent.Closed -> {
                            Json.line("event" to "closed")
                        }
                    },
                )
            }
        fake.use {
            if (!args.flag("no-broadcast")) {
                val to = InetAddress.getByName(args.value("broadcast-to") ?: "255.255.255.255")
                it.broadcast(to)
            }
            out.println(Json.line("event" to "listening", "port" to it.port, "device" to name))
            if (seconds != null) Thread.sleep(seconds * 1000L) else waitForStdinToClose()
        }
        return ExitCodes.OK
    }

    private fun waitForStdinToClose() {
        while (System.`in`.read() >= 0) {
            // Input is ignored; closing it is the signal to stop.
        }
    }
}
