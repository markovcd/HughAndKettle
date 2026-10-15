package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.amp.VolumeTrim
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.concurrent.thread
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

    /** What the link's reader hands the loop when the run is over. */
    private object Ended

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        val amount = args.positional.getOrNull(2)?.toIntOrNull()
        val seconds =
            args.value("seconds")?.let {
                args.int("seconds", 0).getOrElse { failure ->
                    err.println(failure.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            }
        val preset =
            args.value("recall")?.let {
                args.int("recall", 0).getOrElse { failure ->
                    err.println(failure.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            }
        if (target == null || args.positional.size != 3 || (seconds != null && seconds < 1)) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        if (amount == null || amount !in -127..127) {
            err.println("a trim takes -127..127, got '${args.positional[2]}'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(args.positional[1]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
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
        thread(name = "trim-until", isDaemon = true) {
            ListenCommand.await(CountDownLatch(1), seconds)
            heard.put(Ended)
        }
        val started = TimeSource.Monotonic.markNow()

        fun print(vararg fields: Pair<String, Any?>) =
            out.println(
                Json.line("ms" to started.elapsedNow().inWholeMilliseconds, *fields),
            )

        var trim = VolumeTrim.of(volume, amount)
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
                val (muted, messages) = trim.recall(recall, mute)
                trim = muted
                if (!send(messages)) return ExitCodes.NO_LINK
                muteDeadline = started.elapsedNow().inWholeMilliseconds + VolumeTrim.muteLimit.inWholeMilliseconds
            }
            while (true) {
                val wait = muteDeadline?.let { (it - started.elapsedNow().inWholeMilliseconds).coerceAtLeast(0) }
                val next = if (wait == null) heard.take() else heard.poll(wait, TimeUnit.MILLISECONDS)
                if (next == null) {
                    val (unmuted, lift) = trim.unmuted()
                    trim = unmuted
                    muteDeadline = null
                    print("muted_too_long_ms" to VolumeTrim.muteLimit.inWholeMilliseconds)
                    if (!send(listOfNotNull(lift))) return ExitCodes.NO_LINK
                    continue
                }
                val message = next as? MidiMessage ?: break
                describe(message, volume.controller)?.let { print(*it) }
                val answer = trim.restore(message)
                trim = trim.after(message)
                if (trim.muting == null) muteDeadline = null
                if (!send(answer)) return ExitCodes.NO_LINK
            }
        }
        val reason = lostBecause.get() ?: return ExitCodes.OK
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
