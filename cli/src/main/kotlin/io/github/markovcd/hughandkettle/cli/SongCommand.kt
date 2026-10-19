package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import java.io.IOException
import java.io.PrintWriter
import kotlin.io.path.Path
import kotlin.time.TimeSource

/**
 * The songs of each amp model, the way the app keeps them, in a folder, `--dir` or `~/.hk/songs`: a tempo and
 * a run of presets, each held for its beats. `show`, `set` and `play` work on the song `--name` names, else
 * the one open in the app. `play` counts in a bar, unless the song was set with `--no-count-in`, then recalls
 * each preset on its beat with the delay time at one repeat a beat, and holds that time across the recalls,
 * as the tempo lock does.
 */
public object SongCommand : Command {
    override val name: String = "song"
    override val usage: String =
        """
        hk song list <amp-id> [--dir D]
        hk song show <amp-id> [--name N] [--dir D]
        hk song set <amp-id> <bpm> <preset>:<beats> ... [--no-count-in] [--name N] [--dir D]
        hk song play <host[:port]> <amp-id> [--name N] [--dir D]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val verb = args.positional.firstOrNull()
        val rest = args.positional.drop(1)
        val ampAt = if (verb == "play") 1 else 0
        val enough =
            when (verb) {
                "list", "show" -> rest.size == 1
                "set" -> rest.size >= 2
                "play" -> rest.size == 2
                else -> false
            }
        if (!enough) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(rest[ampAt]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val store = SongStore(args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "songs"))
        val list =
            store.list(amp).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_FILE
            }
        if (verb == "list") {
            list.songs.forEach { print(it, it.name.equals(list.open, ignoreCase = true), out) }
            return ExitCodes.OK
        }
        val named = args.value("name")
        val kept =
            if (named != null) {
                list.find(named)
            } else {
                list.open?.let(list::find) ?: list.songs.firstOrNull { it.song != null }
            }
        if (verb == "set") {
            val song =
                parse(amp, rest.drop(1), countIn = !args.flag("no-count-in"))
                    .getOrElse {
                        err.println(it.message)
                        return ExitCodes.BAD_ARGUMENTS
                    }.copy(name = kept?.name ?: named?.trim() ?: "")
            store
                .save(song, replace = true)
                .mapCatching { if (kept == null) store.open(amp, song.name).getOrThrow() }
                .onFailure {
                    err.println(it.message)
                    return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
                }
            print(song, out)
            return ExitCodes.OK
        }
        if (named != null && kept == null) {
            err.println("${amp.id} has no song named '$named'; hk song list names them")
            return ExitCodes.BAD_ARGUMENTS
        }
        kept?.problem?.let {
            err.println(it)
            return ExitCodes.BAD_FILE
        }
        val song = kept?.song ?: Song(amp)
        if (verb == "show") {
            print(song, out)
            return ExitCodes.OK
        }
        if (song.steps.isEmpty()) {
            err.println("${amp.id} has no song to play; hk song set makes one")
            return ExitCodes.BAD_ARGUMENTS
        }
        val target = WmiAddress.parse(rest[0])
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        return play(song, target, out, err)
    }

    /** A song from `<bpm> <preset>:<beats> ...`; anything else is reported, not thrown. */
    private fun parse(
        amp: AmpModel,
        words: List<String>,
        countIn: Boolean,
    ): Result<Song> {
        val tempo =
            words[0].toIntOrNull()?.takeIf { it in Song.TEMPOS }
                ?: return Result.failure(
                    IllegalArgumentException(
                        "a tempo is ${Song.TEMPOS.first}..${Song.TEMPOS.last} BPM, got '${words[0]}'",
                    ),
                )
        val steps =
            words.drop(1).map { word ->
                val parts = word.split(':')
                val preset = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..amp.presetCount }
                val beats = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..SongStep.MOST_BEATS }
                if (parts.size != 2 || preset == null || beats == null) {
                    return Result.failure(
                        IllegalArgumentException(
                            "a step is <preset>:<beats>, preset 1..${amp.presetCount} and beats 1..${SongStep.MOST_BEATS}, got '$word'",
                        ),
                    )
                }
                SongStep(preset, beats)
            }
        return Result.success(Song(amp, tempo, steps, countIn))
    }

    private fun print(
        song: Song,
        out: PrintWriter,
    ) {
        out.println(
            Json.line(
                "amp" to song.amp.id,
                "name" to song.name,
                "tempo" to song.tempo,
                "count_in" to song.countIn,
                "delay_ms" to song.delayMs,
            ),
        )
        song.steps.forEachIndexed { index, step ->
            out.println(Json.line("step" to index + 1, "preset" to step.preset, "beats" to step.beats))
        }
    }

    /** One line of the list: the song's name, whether it is open, and its tempo and length, or why it cannot be read. */
    private fun print(
        kept: KeptSong,
        open: Boolean,
        out: PrintWriter,
    ) {
        val song = kept.song
        if (song == null) {
            out.println(Json.line("name" to kept.name, "open" to open, "problem" to kept.problem))
        } else {
            out.println(
                Json.line(
                    "name" to kept.name,
                    "open" to open,
                    "tempo" to song.tempo,
                    "steps" to song.steps.size,
                    "ms" to song.length.inWholeMilliseconds,
                ),
            )
        }
    }

    /** Plays [song] on the amp behind [target] from now to its last beat, holding its delay time. */
    private fun play(
        song: Song,
        target: WmiAddress,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val link =
            QueuedLink.open(target).getOrElse {
                err.println(it.message)
                return ExitCodes.NO_LINK
            }
        val time = song.delayTime
        var answers =
            RecallAnswers(
                lock =
                    song.amp.controls
                        .firstOrNull(Control::timed)
                        ?.let { TempoLock(it, time?.value) },
            )
        // Measured from the first step, so the count-in reads in negative milliseconds.
        val started = TimeSource.Monotonic.markNow() + song.lead
        val counts = if (song.countIn) Song.COUNT_IN_BEATS else 0

        fun ms() = started.elapsedNow().inWholeMilliseconds
        link.use {
            fun send(messages: List<MidiMessage>): Boolean {
                for (message in messages) {
                    it.send(message).getOrElse { failure ->
                        err.println("send failed: ${failure.message}")
                        return false
                    }
                }
                return true
            }
            var next = 0
            var counted = 0
            while (true) {
                if (counted < counts && started.elapsedNow() >= song.beat * counted - song.lead) {
                    out.println(Json.line("ms" to ms(), "count" to counts - counted))
                    out.flush()
                    counted++
                    continue
                }
                if (next < song.steps.size && started.elapsedNow() >= song.startOf(next)) {
                    val cue = song.cue(next)
                    answers = answers.sent(cue)
                    if (!send(cue)) return ExitCodes.NO_LINK
                    out.println(
                        Json.line(
                            "ms" to ms(),
                            "step" to next + 1,
                            "preset" to song.steps[next].preset,
                            "delay_ms" to song.delayMs,
                        ),
                    )
                    out.flush()
                    next++
                    continue
                }
                val until =
                    when {
                        counted < counts -> song.beat * counted - song.lead
                        next < song.steps.size -> song.startOf(next)
                        else -> song.length
                    }
                val wait = (until - started.elapsedNow()).inWholeMilliseconds
                if (next >= song.steps.size && wait <= 0) break
                val message =
                    when (val polled = link.next(wait)) {
                        is QueuedLink.Next.Heard -> polled.message
                        QueuedLink.Next.Quiet -> continue
                        QueuedLink.Next.Over -> break
                    }
                val answered = answers.heard(message)
                answers = answered.answers
                val restore = answered.restored ?: continue
                if (!send(listOf(restore))) return ExitCodes.NO_LINK
                out.println(Json.line("ms" to ms(), "restored" to restore.value, "delay_ms" to song.delayMs))
            }
        }
        link.lost?.let {
            err.println("link lost: $it")
            return ExitCodes.NO_LINK
        }
        out.println(Json.line("ms" to ms(), "ended" to true))
        return ExitCodes.OK
    }
}
