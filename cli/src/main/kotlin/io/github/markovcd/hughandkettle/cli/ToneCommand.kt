package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.CappedRead
import io.github.markovcd.hughandkettle.amp.PresetReading
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneImport
import io.github.markovcd.hughandkettle.amp.ToneStore
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.Path
import kotlin.io.path.inputStream
import kotlin.io.path.writeText

/**
 * Keeps tones in a folder, `--dir` or `~/.hk/tones`, the way the app keeps them on the phone: save
 * from values typed as `hk set` takes them, read from the amp's presets, recall onto an amp, list,
 * rename, reorder, pin, delete, and move them between machines as files. A tone renamed is renamed in the
 * steps of the songs in `--songs` or `~/.hk/songs` too.
 */
public object ToneCommand : Command {
    override val name: String = "tone"
    override val usage: String =
        """
        hk tone list [--dir D]
        hk tone save <amp-id> <name> [<control-id>=<value> ...] [--replace] [--dir D]
        hk tone recall ${AmpTarget.USAGE} <name> [--dir D]
        hk tone read ${AmpTarget.USAGE} <amp-id> [<preset>[-<preset>]] [--replace] [--dir D] [--names D]
        hk tone rename <name> <new-name> [--dir D] [--songs D]
        hk tone move <name> <position> [--dir D]
        hk tone pin|unpin <name> [--dir D]    (a pinned tone is one the app's − and + step through)
        hk tone delete <name> [--dir D]
        hk tone export <name> <file> [--dir D]
        hk tone import <file> [--replace] [--dir D]    (a tone file, or a GrandMeister .gm40memory)
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val store = ToneStore(args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "tones"))
        val verb = args.positional.firstOrNull()
        val rest = args.positional.drop(1)
        val code =
            when (verb) {
                "list" -> if (rest.isEmpty()) list(store, out, err) else null
                "save" -> if (rest.size >= 2) save(store, rest, args.flag("replace"), out, err) else null
                "recall" -> if (rest.size == 2) recall(store, rest[0], rest[1], out, err) else null
                "read" -> if (rest.size in 2..3) read(store, args, rest, out, err) else null
                "rename" -> if (rest.size == 2) rename(store, songs(args), rest[0], rest[1], out, err) else null
                "move" -> if (rest.size == 2) move(store, rest[0], rest[1], out, err) else null
                "pin", "unpin" -> if (rest.size == 1) pin(store, rest[0], verb == "pin", out, err) else null
                "delete" -> if (rest.size == 1) delete(store, rest[0], out, err) else null
                "export" -> if (rest.size == 2) export(store, rest[0], Path(rest[1]), out, err) else null
                "import" -> if (rest.size == 1) import(store, Path(rest[0]), args.flag("replace"), out, err) else null
                else -> null
            }
        if (code != null) return code
        err.println(usage)
        return ExitCodes.BAD_ARGUMENTS
    }

    private fun list(
        store: ToneStore,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val list = store.list()
        list.tones.forEachIndexed { index, tone ->
            out.println(
                Json.line(
                    "position" to index + 1,
                    *fields(tone),
                    "pinned" to tone.pinned,
                ),
            )
        }
        list.problems.forEach { err.println("skipped $it") }
        return ExitCodes.OK
    }

    private fun save(
        store: ToneStore,
        rest: List<String>,
        replace: Boolean,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val amp = AmpArgument.parse(rest[0]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val values = LinkedHashMap<String, Int>()
        for (pair in rest.drop(2)) {
            val id = pair.substringBefore('=')
            val control = amp.control(id)
            if ('=' !in pair || control == null) {
                err.println(
                    "'$pair' is not <control-id>=<value>; ${amp.id} has ${amp.controls.joinToString(", ") { it.id }}",
                )
                return ExitCodes.BAD_ARGUMENTS
            }
            values[id] =
                ControlValue
                    .parse(control, pair.substringAfter('='))
                    .orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
                    .value
        }
        if (rest[1].isBlank()) {
            err.println("a tone needs a name")
            return ExitCodes.BAD_ARGUMENTS
        }
        val tone = Tone(rest[1], amp, values)
        return changed(store.save(tone, replace), out, err, "saved" to tone.name, *fields(tone).drop(1).toTypedArray())
    }

    private fun recall(
        store: ToneStore,
        address: String,
        name: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = AmpTarget.parse(address)
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        val messages = tone.recall()
        link.use { it.sendAll(messages) }.orExit(err, ExitCodes.NO_LINK) { return it }
        out.println(
            Json.line(
                *link.where.toTypedArray(),
                "tone" to tone.name,
                "amp" to tone.amp.id,
                "sent" to messages.size,
            ),
        )
        return ExitCodes.OK
    }

    /**
     * Recalls each preset in turn and saves what the amp's value block says of it as a tone named
     * after the preset. A preset whose block does not come in [PER_PRESET_SECONDS] is missed and said.
     */
    private fun read(
        store: ToneStore,
        args: Options,
        rest: List<String>,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = AmpTarget.parse(rest[0])
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(rest[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val presets = presets(amp, rest.getOrNull(2)).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val names = PresetCommand.store(args).names(amp).orExit(err, ExitCodes.BAD_FILE) { return it }
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        var reading = PresetReading.of(names, presets)
        link.use {
            while (true) {
                val preset = reading.preset ?: break
                it.sendAll(listOfNotNull(reading.next)).orExit(err, ExitCodes.NO_LINK) { code -> return code }
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(PER_PRESET_SECONDS)
                while (reading.preset == preset) {
                    val wait = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
                    reading =
                        when (val next = it.next(wait)) {
                            is QueuedLink.Next.Heard -> reading.after(next.message)
                            QueuedLink.Next.Quiet -> reading.skip()
                            QueuedLink.Next.Over -> break
                        }
                }
                link.lost?.let { reason ->
                    err.println("link lost: $reason")
                    return ExitCodes.NO_LINK
                }
                if (preset in reading.missed) {
                    err.println("preset $preset: the amp sent no values in $PER_PRESET_SECONDS s")
                } else {
                    out.println(Json.line("preset" to preset, *fields(reading.tones.last())))
                }
            }
        }
        if (reading.tones.isEmpty()) return ExitCodes.NOTHING_FOUND
        return changed(
            store.save(reading.tones, args.flag("replace")),
            out,
            err,
            "read" to reading.tones.size,
            "missed" to reading.missed,
        )
    }

    /** The presets `[<preset>[-<preset>]]` names, or every one [amp] has. */
    private fun presets(
        amp: AmpModel,
        text: String?,
    ): Result<List<Int>> {
        if (text == null) return Result.success((1..amp.presetCount).toList())
        val first = text.substringBefore('-').toIntOrNull()
        val last = if ('-' in text) text.substringAfter('-').toIntOrNull() else first
        if (first == null || last == null || first > last || first < 1 || last > amp.presetCount) {
            return Result.failure(
                IllegalArgumentException(
                    "'$text' is not a preset or a range of them in ${amp.name}'s 1..${amp.presetCount}",
                ),
            )
        }
        return Result.success((first..last).toList())
    }

    /** The folder of songs [args] point at with `--songs`, whose steps follow a tone renamed. */
    private fun songs(args: Options): SongStore =
        SongStore(args.value("songs")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "songs"))

    /** Renames a tone, and the steps on it in every song of its amp. */
    private fun rename(
        store: ToneStore,
        songs: SongStore,
        name: String,
        to: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        val renamed = store.rename(tone.name, to).mapCatching { songs.renameTone(tone.amp, tone.name, to).getOrThrow() }
        return changed(renamed, out, err, "renamed" to tone.name, "to" to to)
    }

    private fun pin(
        store: ToneStore,
        name: String,
        on: Boolean,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        return changed(store.pin(tone.name, on), out, err, "name" to tone.name, "pinned" to on)
    }

    private fun move(
        store: ToneStore,
        name: String,
        position: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val to = position.toIntOrNull()
        if (to == null) {
            err.println("the position must be a whole number, got '$position'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        return changed(store.move(tone.name, to), out, err, "moved" to tone.name, "position" to to)
    }

    private fun delete(
        store: ToneStore,
        name: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        return changed(store.delete(tone.name), out, err, "deleted" to tone.name)
    }

    private fun export(
        store: ToneStore,
        name: String,
        file: Path,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        try {
            file.writeText(ToneFile.write(tone))
        } catch (e: IOException) {
            err.println("cannot write $file: ${e.message}")
            return ExitCodes.BAD_FILE
        }
        out.println(Json.line("exported" to tone.name, "file" to file.toString()))
        return ExitCodes.OK
    }

    private fun import(
        store: ToneStore,
        file: Path,
        replace: Boolean,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val bytes =
            try {
                file.inputStream().use { CappedRead.bytes(it, ToneImport.MOST_BYTES) }
            } catch (e: IOException) {
                err.println("cannot read $file: ${e.message}")
                return ExitCodes.BAD_FILE
            }
        if (bytes == null) {
            err.println("$file is larger than a tone file can be")
            return ExitCodes.BAD_FILE
        }
        val read =
            ToneImport.read(bytes).getOrElse {
                err.println("$file: ${it.message}")
                return ExitCodes.BAD_FILE
            }
        read.skipped.forEach { err.println("$file: skipped: $it") }
        if (!ToneImport.isPresetFile(bytes)) {
            val tone = read.tones.single()
            return changed(store.save(tone, replace), out, err, "imported" to tone.name, "skipped" to read.skipped)
        }
        // A preset file's tones are named apart from each other, and from the tones kept unless they replace them.
        val tones = read.namedApartFrom(if (replace) emptyList() else store.list().tones.map { it.name }).tones
        val saved = store.save(tones, replace)
        saved.onSuccess { tones.forEach { out.println(Json.line(*fields(it))) } }
        return changed(saved, out, err, "presets" to tones.size, "skipped" to read.skipped)
    }

    private fun found(
        store: ToneStore,
        name: String,
        err: PrintWriter,
    ): Tone? =
        store.find(name).also { tone ->
            if (tone ==
                null
            ) {
                err.println(
                    "no tone '$name'; there are ${store.list().tones.joinToString(", ") { "'${it.name}'" }}",
                )
            }
        }

    /** Prints [fields] for a change the store made, or says why it did not. */
    private fun changed(
        result: Result<Unit>,
        out: PrintWriter,
        err: PrintWriter,
        vararg fields: Pair<String, Any?>,
    ): Int {
        result.onFailure {
            err.println(it.message)
            return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
        }
        out.println(Json.line(*fields))
        return ExitCodes.OK
    }

    private fun fields(tone: Tone): Array<Pair<String, Any?>> =
        arrayOf("name" to tone.name, "amp" to tone.amp.id, "values" to tone.values)

    // The amp answers a recall in tens of milliseconds over the WMI-1; this leaves room for a busy network.
    private const val PER_PRESET_SECONDS = 2L
}
