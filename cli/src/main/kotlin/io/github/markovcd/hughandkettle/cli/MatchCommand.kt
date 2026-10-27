package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.ChordListen
import io.github.markovcd.hughandkettle.amp.JavaMicrophone
import io.github.markovcd.hughandkettle.amp.LoudnessCorrectionStore
import io.github.markovcd.hughandkettle.amp.LoudnessMatch
import io.github.markovcd.hughandkettle.amp.LoudnessMatch.Stage
import io.github.markovcd.hughandkettle.amp.MatchResult
import io.github.markovcd.hughandkettle.amp.Microphone
import io.github.markovcd.hughandkettle.amp.MicrophoneInput
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.amp.VolumeTrim
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import java.io.IOException
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.time.TimeSource

/**
 * The loudness match, as the app's soundcheck does it, listening through this machine's [microphone]: `run`
 * recalls the `--reference` preset and then each of `--presets`, asks on stderr for a chord each time, moves the
 * volume until the preset is as loud as the reference, prints one line per preset on stdout and keeps the
 * corrections in a folder, `--dir` or `~/.hk/loudness`, which the app reads too, unless `--no-keep`. `show` prints
 * the corrections kept, and `clear` takes one or all away.
 */
public class MatchCommand(
    private val microphone: Microphone = JavaMicrophone,
) : Command {
    override val name: String = "match"
    override val usage: String =
        """
        hk match run ${AmpTarget.USAGE} <amp-id> --presets 5,7,12 [--reference 5] [--no-keep] [--dir D]
        hk match show <amp-id> [--dir D]
        hk match clear <amp-id> [<preset>] [--dir D]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val verb = args.positional.firstOrNull()
        val rest = args.positional.drop(1)
        val enough =
            when (verb) {
                "run" -> rest.size == 2
                "show" -> rest.size == 1
                "clear" -> rest.size in 1..2
                else -> false
            }
        if (!enough) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument
                .parse(
                    rest[
                        if (verb ==
                            "run"
                        ) {
                            1
                        } else {
                            0
                        },
                    ],
                ).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val store =
            LoudnessCorrectionStore(
                args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "loudness"),
            )
        val kept = store.corrections(amp).orExit(err, ExitCodes.BAD_FILE) { return it }
        return when (verb) {
            "show" -> {
                kept.amounts.toSortedMap().forEach { (preset, amount) ->
                    out.println(Json.line("preset" to preset, "correction" to amount))
                }
                ExitCodes.OK
            }

            "clear" -> {
                val preset =
                    rest.getOrNull(1)?.let {
                        it.toIntOrNull()
                            ?: return bad(err, "the preset must be a whole number, got '$it'")
                    }
                val after = if (preset == null) kept.copy(amounts = emptyMap()) else kept.without(preset)
                store.keep(after).orExit(err, ExitCodes.BAD_FILE) { return it }
                out.println(Json.line("amp" to amp.id, "cleared" to (preset ?: "all")))
                ExitCodes.OK
            }

            else -> {
                soundcheck(args, rest[0], amp, store, out, err)
            }
        }
    }

    private fun soundcheck(
        args: Options,
        targetArgument: String,
        amp: AmpModel,
        store: LoudnessCorrectionStore,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = AmpTarget.parse(targetArgument) ?: return bad(err, usage)
        val presets =
            args.value("presets")?.split(",")?.map {
                it.trim().toIntOrNull()
                    ?: return bad(err, "--presets takes numbers, got '$it'")
            }
                ?: return bad(err, "--presets names the presets to match, as 5,7,12")
        val outside = presets.filter { it !in 1..amp.presetCount }
        if (outside.isNotEmpty()) return bad(err, "${amp.name} has presets 1..${amp.presetCount}, not $outside")
        val reference =
            args.optionalInt("reference").orExit(err, ExitCodes.BAD_ARGUMENTS) { return it } ?: presets.first()
        if (reference !in
            1..amp.presetCount
        ) {
            return bad(err, "${amp.name} has presets 1..${amp.presetCount}, not $reference")
        }
        val volume = amp.control("volume")
        val mute = amp.control("mute")
        if (volume == null || mute == null) return bad(err, "${amp.id} has no volume and mute to match with")
        val kept = store.corrections(amp).orExit(err, ExitCodes.BAD_FILE) { return it }
        val input = microphone.open().orExit(err, ExitCodes.NOTHING_FOUND) { return it }
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        var answers = RecallAnswers(trim = VolumeTrim.of(volume).corrected(kept.amounts))
        var match = LoudnessMatch.of(presets + reference, reference)
        var playing: Int? = null
        val started = TimeSource.Monotonic.markNow()
        link.use {
            input.use {
                fun send(messages: List<MidiMessage>): Boolean =
                    link
                        .sendAll(messages) { message ->
                            if (message is ControlChange &&
                                message.controller == volume.controller
                            ) {
                                playing = message.value
                            }
                        }.onFailure { failure -> err.println(failure.message) }
                        .isSuccess

                /** Answers what the amp says, as the app does, until a value block of [preset] has been answered. */
                fun awaitBlock(preset: Int): Boolean {
                    val deadline = started.elapsedNow().inWholeMilliseconds + ANSWER_LIMIT_MS
                    while (true) {
                        val wait = deadline - started.elapsedNow().inWholeMilliseconds
                        val message =
                            (link.next(wait.coerceAtLeast(0)) as? QueuedLink.Next.Heard)?.message ?: return false
                        val answered = answers.heard(message)
                        answers = answered.answers
                        val block = (message as? SystemExclusive)?.let(ValueBlock::parse)
                        if (block != null) playing = block.values[volume.controller]?.let(volume::fromReport)
                        if (!send(answered.replies)) return false
                        if (block?.preset == preset) return true
                    }
                }

                while (true) {
                    val now = match
                    when (val stage = now.stage) {
                        Stage.Recalling -> {
                            val preset = now.at!!
                            val (behind, messages) = answers.recall(amp.recall(preset), mute, muted = false)
                            answers = behind
                            if (!send(messages)) return ExitCodes.NO_LINK
                            val shift = if (awaitBlock(preset)) answers.trim?.shift else null
                            val heard = playing
                            if (shift == null || heard == null) {
                                err.println("the amp did not answer the recall of preset $preset")
                                match = now.stop()
                                continue
                            }
                            match = now.recalled(shift.stored, shift.correction, heard)
                            err.println(
                                "preset $preset" + (if (preset == reference) " (the reference)" else "") +
                                    ": play one chord and let it ring",
                            )
                        }

                        is Stage.Playing, is Stage.Retry -> {
                            val outcome = listen(input).orExit(err, ExitCodes.NOTHING_FOUND) { return it }
                            if (outcome is ChordListen.Outcome.Retry) {
                                err.println("${words(outcome.why)}: play the same chord again")
                            }
                            val (next, aimed) = now.heard(outcome)
                            match = next
                            aimed?.let { if (!send(listOf(volume.set(it)))) return ExitCodes.NO_LINK }
                        }

                        is Stage.Adjusting -> {
                            val off = (stage.off * 10).roundToInt() / 10.0
                            err.println(
                                "${if (off > 0) "too loud" else "too quiet"} by ${abs(off)} dB: " +
                                    "volume ${stage.from} -> ${stage.to}, stop and wait",
                            )
                            repeat(SETTLE_FRAMES) { input.read().orExit(err, ExitCodes.NOTHING_FOUND) { return it } }
                            err.println("play the same chord again")
                            match = now.adjusted()
                        }

                        Stage.Done -> {
                            break
                        }
                    }
                }
            }
        }
        match.results.forEach { out.println(Json.line(*fields(it))) }
        if (args.flag("no-keep")) return ExitCodes.OK
        store.keep(kept.with(match.corrections())).onFailure {
            err.println(it.message)
            return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
        }
        return ExitCodes.OK
    }

    /** Listens for one chord on [input]. */
    private fun listen(input: MicrophoneInput): Result<ChordListen.Outcome> {
        var listen = ChordListen.start()
        while (true) {
            listen = listen.after(input.read().getOrElse { return Result.failure(it) })
            listen.outcome?.let { return Result.success(it) }
        }
    }

    private fun words(why: ChordListen.Why): String =
        when (why) {
            ChordListen.Why.TooQuiet -> "too quiet to measure"
            ChordListen.Why.TooLoud -> "too loud for the microphone"
            ChordListen.Why.TooNoisy -> "too much other sound around the chord"
        }

    private fun fields(result: MatchResult): Array<Pair<String, Any?>> =
        when (result) {
            is MatchResult.Reference -> {
                arrayOf(
                    "preset" to result.preset,
                    "result" to "reference",
                    "stored" to result.stored,
                    "volume" to result.volume,
                )
            }

            is MatchResult.Matched -> {
                arrayOf(
                    "preset" to result.preset,
                    "result" to if (result.reached) "matched" else "limit",
                    "stored" to result.stored,
                    "volume" to result.volume,
                    "correction" to result.correction,
                    "off_db" to (result.off * 10).roundToInt() / 10.0,
                )
            }

            is MatchResult.Skipped -> {
                arrayOf("preset" to result.preset, "result" to "skipped", "stored" to result.stored)
            }
        }

    private fun bad(
        err: PrintWriter,
        why: String,
    ): Int {
        err.println(why)
        return ExitCodes.BAD_ARGUMENTS
    }

    private companion object {
        // How long a recall waits for the amp's value block, as the app waits.
        const val ANSWER_LIMIT_MS = 3_000L

        // Frames let pass after the volume moves, for the old chord to die: a second and a half.
        const val SETTLE_FRAMES = 30
    }
}
