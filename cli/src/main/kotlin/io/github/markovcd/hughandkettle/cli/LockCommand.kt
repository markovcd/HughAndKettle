package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
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

    /** What the link's reader hands the loop when the run is over. */
    private object Ended

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val seconds =
            args.value("seconds")?.let {
                args.int("seconds", 0).getOrElse { failure ->
                    err.println(failure.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            }
        if (target == null || args.positional.size !in 2..3 || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(args.positional[1]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val time = amp.controls.firstOrNull { it.scale?.unit == "ms" }
        val scale = time?.scale
        if (time == null || scale == null) {
            err.println("${amp.id} has no delay time to hold")
            return ExitCodes.BAD_ARGUMENTS
        }
        val given =
            args.positional.getOrNull(2)?.let { text ->
                ControlValue.parse(time, text).getOrElse {
                    err.println(it.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            }
        val heard = LinkedBlockingQueue<Any>()
        val lostBecause = AtomicReference<String?>()
        val link =
            WmiLink
                .open(
                    target.host,
                    target.port,
                    onMessage = heard::put,
                    onClosed = {
                        lostBecause.set(it)
                        heard.put(Ended)
                    },
                ).getOrElse {
                    err.println("cannot reach $target: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        thread(name = "lock-until", isDaemon = true) {
            ListenCommand.await(CountDownLatch(1), seconds)
            heard.put(Ended)
        }
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
        var lock = TempoLock(time, given?.value)
        lock.value?.let { print("held", it) }
        link.use {
            while (true) {
                val message = heard.take() as? MidiMessage ?: break
                val before = lock.value
                lock = lock.after(message)
                lock.value?.takeIf { it != before }?.let { value -> print("held", value) }
                val restore = lock.restore(message) ?: continue
                it.send(restore).getOrElse { failure ->
                    err.println("send failed: ${failure.message}")
                    return ExitCodes.NO_LINK
                }
                print("restored", restore.value)
            }
        }
        val reason = lostBecause.get() ?: return ExitCodes.OK
        err.println("link lost: $reason")
        return ExitCodes.NO_LINK
    }
}
