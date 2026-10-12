package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * Measures the app's own share of a knob move: from handing a message to the link to a fake WMI-1 on
 * this machine parsing it, one message at a time, after a warm-up. WiFi and the box are not in it.
 */
public object LatencyCommand : Command {
    override val name: String = "latency"
    override val usage: String = "hk latency [--count N]"

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val count =
            args.int("count", DEFAULT_COUNT).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        if (count !in 1..MAX_COUNT || args.positional.isNotEmpty()) {
            err.println(usage)
            err.println("--count is 1..$MAX_COUNT")
            return ExitCodes.BAD_ARGUMENTS
        }
        val arrived = LinkedBlockingQueue<Long>()
        FakeWmi(onEvent = { if (it is FakeWmiEvent.Received) arrived.put(System.nanoTime()) }).use { fake ->
            val link =
                WmiLink.open("127.0.0.1", fake.port).getOrElse {
                    err.println("cannot reach the fake WMI-1: ${it.message}")
                    return ExitCodes.NO_LINK
                }
            link.use {
                val nanos =
                    (0 until WARM_UP + count).map { i ->
                        val sent = System.nanoTime()
                        it.send(ControlChange(MidiChannel.Default, GAIN, i % 128)).getOrElse { failure ->
                            err.println("send failed: ${failure.message}")
                            return ExitCodes.NO_LINK
                        }
                        val at =
                            arrived.poll(1, TimeUnit.SECONDS) ?: run {
                                err.println("message ${i + 1} never arrived")
                                return ExitCodes.NO_LINK
                            }
                        at - sent
                    }
                val sorted = nanos.drop(WARM_UP).sorted()

                fun micros(fraction: Double): Long = sorted[((sorted.size - 1) * fraction).toInt()] / 1000
                out.println(
                    Json.line(
                        "count" to count,
                        "min_us" to micros(0.0),
                        "median_us" to micros(0.5),
                        "p95_us" to micros(0.95),
                        "max_us" to micros(1.0),
                    ),
                )
            }
        }
        return ExitCodes.OK
    }

    private const val DEFAULT_COUNT = 1000
    private const val MAX_COUNT = 100_000

    // Enough sends for the JIT to compile the send and parse paths before anything is timed.
    private const val WARM_UP = 200
    private const val GAIN = 20
}
