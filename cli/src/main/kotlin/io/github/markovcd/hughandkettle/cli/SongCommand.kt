package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.CappedRead
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongFile
import io.github.markovcd.hughandkettle.amp.SongImport
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.midi.MidiMessage
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText
import kotlin.time.TimeSource

/**
 * The songs of each amp model, the way the app keeps them, in a folder, `--dir` or `~/.hk/songs`: a tempo and
 * a run of presets and tones, each held for its beats. `show`, `set` and `play` work on the song `--name` names,
 * else the one open in the app. `play` counts in a bar, unless the song was set with `--no-count-in`, then
 * recalls each preset, or sends each tone kept in `--tones` or `~/.hk/tones`, on its beat. With `--lock` it sets
 * the delay time to a `--note` (a quarter unless given) at the song's tempo and holds it across the recalls, as
 * the app does while its tempo lock is on; a step whose tone is missing then sets the delay time alone. The rest of the verbs keep the list as the
 * app's song list does.
 */
public object SongCommand : Command {
    override val name: String = "song"
    override val usage: String =
        """
        hk song list <amp-id> [--dir D]
        hk song show <amp-id> [--name N] [--dir D]
        hk song new|open|duplicate|delete <amp-id> <name> [--dir D]
        hk song rename <amp-id> <name> <new-name> [--dir D]
        hk song move <amp-id> <name> <position> [--dir D]
        hk song export <amp-id> <name> <file> [--dir D]
        hk song import <amp-id> <file> [--dir D]    (a song for another amp model is refused)
        hk song set <amp-id> <bpm> <preset>:<beats>|tone:<name>:<beats>[@<note>] ... [--no-count-in] [--name N] [--dir D]
        hk song play ${AmpTarget.USAGE} <amp-id> [--lock [--note 1/8d]] [--name N] [--dir D] [--tones D]
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
                "play", "import", "new", "open", "duplicate", "delete" -> rest.size == 2
                "rename", "move", "export" -> rest.size == 3
                else -> false
            }
        if (!enough) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(rest[ampAt]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val store = SongStore(args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "songs"))
        when (verb) {
            "import" -> return import(store, amp, Path(rest[1]), out, err)

            "new" -> return create(store, amp, rest[1], out, err)

            "open", "duplicate", "delete", "rename", "move", "export" -> return library(
                store,
                amp,
                verb,
                rest.drop(1),
                out,
                err,
            )
        }
        val list = store.list(amp).orExit(err, ExitCodes.BAD_FILE) { return it }
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
                    .orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
                    .copy(name = kept?.name ?: named?.trim() ?: "")
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
        val target = AmpTarget.parse(rest[0])
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val note = NoteArgument.parse(args).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        return play(song, tones(args), target, note.takeIf { args.flag("lock") }, out, err)
    }

    /** Keeps an empty song called [name] at the end of [amp]'s list and opens it, as the app's New song does. */
    private fun create(
        store: SongStore,
        amp: AmpModel,
        name: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val song = Song(amp, name = name.trim())
        if (song.name.isEmpty()) {
            err.println("a song needs a name")
            return ExitCodes.BAD_ARGUMENTS
        }
        return changed(
            store.save(song).mapCatching { store.open(amp, song.name).getOrThrow() },
            out,
            err,
            "created" to song.name,
        )
    }

    /** The verbs that act on one kept song by name: open, duplicate, delete, rename, move and export. */
    private fun library(
        store: SongStore,
        amp: AmpModel,
        verb: String,
        words: List<String>,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val list = store.list(amp).orExit(err, ExitCodes.BAD_FILE) { return it }
        val kept = list.find(words[0])
        if (kept == null) {
            err.println("${amp.id} has no song named '${words[0]}'; hk song list names them")
            return ExitCodes.NOTHING_FOUND
        }
        val name = kept.name
        return when (verb) {
            "open" -> {
                changed(store.open(amp, name), out, err, "opened" to name)
            }

            "duplicate" -> {
                store
                    .duplicate(amp, name)
                    .map { copy -> Json.line("duplicated" to name, "copy" to copy) }
                    .let { printed(it, out, err) }
            }

            "delete" -> {
                changed(store.delete(amp, name), out, err, "deleted" to name)
            }

            "rename" -> {
                val to = words[1].trim()
                changed(store.rename(amp, name, to), out, err, "renamed" to name, "to" to to)
            }

            "move" -> {
                val position = words[1].toIntOrNull()
                if (position == null) {
                    err.println("the position must be a whole number, got '${words[1]}'")
                    return ExitCodes.BAD_ARGUMENTS
                }
                changed(store.move(amp, name, position), out, err, "moved" to name, "position" to position)
            }

            else -> {
                export(kept, Path(words[1]), out, err)
            }
        }
    }

    private fun export(
        kept: KeptSong,
        file: Path,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val song = kept.song
        if (song == null) {
            err.println(kept.problem)
            return ExitCodes.BAD_FILE
        }
        try {
            file.writeText(SongFile.write(song))
        } catch (e: IOException) {
            err.println("cannot write $file: ${e.message}")
            return ExitCodes.BAD_FILE
        }
        out.println(Json.line("exported" to kept.name, "file" to file.toString()))
        return ExitCodes.OK
    }

    /** Keeps the song in [file] at the end of [amp]'s list, named apart from the rest, as the app's Import does. */
    private fun import(
        store: SongStore,
        amp: AmpModel,
        file: Path,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val bytes =
            try {
                file.inputStream().use { CappedRead.bytes(it, SongFile.MOST_BYTES) }
            } catch (e: IOException) {
                err.println("cannot read $file: ${e.message}")
                return ExitCodes.BAD_FILE
            }
        if (bytes == null) {
            err.println("$file is larger than a song file can be")
            return ExitCodes.BAD_FILE
        }
        val imported =
            store.import(amp, bytes.decodeToString()).getOrElse {
                err.println("$file: ${it.message}")
                return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
            }
        return when (imported) {
            is SongImport.OtherAmp -> {
                err.println("$file is a song for ${imported.amp}, not ${amp.id}")
                ExitCodes.BAD_FILE
            }

            is SongImport.Kept -> {
                out.println(Json.line("imported" to imported.name, "asked" to imported.asked))
                ExitCodes.OK
            }
        }
    }

    /** Prints [fields] for a change the store made, or says why it did not. */
    private fun changed(
        result: Result<Unit>,
        out: PrintWriter,
        err: PrintWriter,
        vararg fields: Pair<String, Any?>,
    ): Int = printed(result.map { Json.line(*fields) }, out, err)

    /** Prints the [line] a change made, or says why it was not made. */
    private fun printed(
        line: Result<String>,
        out: PrintWriter,
        err: PrintWriter,
    ): Int =
        line.fold(
            {
                out.println(it)
                ExitCodes.OK
            },
            {
                err.println(it.message)
                if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
            },
        )

    /** The folder of tones [args] point at with `--tones`. */
    private fun tones(args: Options): ToneStore =
        ToneStore(args.value("tones")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "tones"))

    /**
     * A song from `<bpm> <preset>:<beats> tone:<name>:<beats> ...`, each step taking `@<note>` for a note length of
     * its own; anything else is reported, not thrown.
     */
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
            words.drop(1).map { given ->
                val word = given.substringBeforeLast('@')
                val note =
                    given.substringAfterLast('@', "").takeIf { '@' in given }?.let { text ->
                        NoteLength.of(text)
                            ?: return Result.failure(
                                IllegalArgumentException("no note '$text' in '$given'; there are ${NoteLength.IDS}"),
                            )
                    }
                step(amp, word).getOrElse { return Result.failure(it) }.copy(note = note)
            }
        return Result.success(Song(amp, tempo, steps, countIn))
    }

    /** One step from `<preset>:<beats>` or `tone:<name>:<beats>`. */
    private fun step(
        amp: AmpModel,
        word: String,
    ): Result<SongStep> {
        if (word.startsWith(TONE)) {
            val name = word.removePrefix(TONE).substringBeforeLast(':', "").trim()
            val beats = word.substringAfterLast(':').toIntOrNull()?.takeIf { it in 1..SongStep.MOST_BEATS }
            if (name.isEmpty() || beats == null) {
                return Result.failure(
                    IllegalArgumentException(
                        "a tone step is tone:<name>:<beats>, beats 1..${SongStep.MOST_BEATS}, got '$word'",
                    ),
                )
            }
            return Result.success(SongStep(beats = beats, tone = name))
        }
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
        return Result.success(SongStep(preset, beats))
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
                "delay_ms" to song.delayMs(NoteLength.QUARTER),
            ),
        )
        song.steps.forEachIndexed { index, step ->
            val fields = listOf("step" to index + 1, sound(step), "beats" to step.beats)
            out.println(Json.line(*(fields + listOfNotNull(step.note?.let { "note" to it.id })).toTypedArray()))
        }
    }

    /** What a step plays, as its line names it: `"preset": 5` or `"tone": "Lead solo"`. */
    private fun sound(step: SongStep): Pair<String, Any?> = step.tone?.let { "tone" to it } ?: ("preset" to step.preset)

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

    /** Plays [song] on the amp behind [target] from now to its last beat, holding the delay time at [note] if given; tones come from [tones]. */
    private fun play(
        song: Song,
        tones: ToneStore,
        target: AmpTarget,
        note: NoteLength?,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        val time = note?.let(song::delayTime)
        val delayMs = note?.let { arrayOf("delay_ms" to song.delayMs(it)) } ?: emptyArray()
        var answers =
            RecallAnswers(
                lock =
                    song.amp.controls
                        .firstOrNull(Control::timed)
                        ?.takeIf { note != null }
                        ?.let { TempoLock(it, time?.value) },
            )
        // Measured from the first step, so the count-in reads in negative milliseconds.
        val started = TimeSource.Monotonic.markNow() + song.lead
        val counts = if (song.countIn) Song.COUNT_IN_BEATS else 0

        fun ms() = started.elapsedNow().inWholeMilliseconds
        link.use {
            fun send(messages: List<MidiMessage>): Boolean =
                it.sendAll(messages).onFailure { failure -> err.println(failure.message) }.isSuccess
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
                    val step = song.steps[next]
                    val tone = step.tone?.let(tones::find)?.takeIf { it.amp == song.amp }
                    val cue = song.cue(next, note) { tone }
                    answers = answers.sent(cue)
                    if (!send(cue)) return ExitCodes.NO_LINK
                    val missing = if (step.tone != null && tone == null) arrayOf("missing" to true) else emptyArray()
                    out.println(
                        Json.line(
                            "ms" to ms(),
                            "step" to next + 1,
                            sound(step),
                            *missing,
                            *delayMs,
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
                out.println(Json.line("ms" to ms(), "restored" to restore.value, *delayMs))
            }
        }
        link.lost?.let {
            err.println("link lost: $it")
            return ExitCodes.NO_LINK
        }
        out.println(Json.line("ms" to ms(), "ended" to true))
        return ExitCodes.OK
    }

    private const val TONE = "tone:"
}
