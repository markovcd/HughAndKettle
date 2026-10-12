package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.IOException
import java.io.PrintWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * Keeps tones in a folder, `--dir` or `~/.hk/tones`, the way the app keeps them on the phone: save
 * from values typed as `hk set` takes them, recall onto an amp, list, rename, reorder, delete, and
 * move them between machines as files.
 */
public object ToneCommand : Command {
    override val name: String = "tone"
    override val usage: String =
        """
        hk tone list [--dir D]
        hk tone save <amp-id> <name> [<control-id>=<value> ...] [--replace] [--dir D]
        hk tone recall <host[:port]> <name> [--dir D]
        hk tone rename <name> <new-name> [--dir D]
        hk tone move <name> <position> [--dir D]
        hk tone delete <name> [--dir D]
        hk tone export <name> <file> [--dir D]
        hk tone import <file> [--replace] [--dir D]
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val store = ToneStore(args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "tones"))
        val rest = args.positional.drop(1)
        val code =
            when (args.positional.firstOrNull()) {
                "list" -> if (rest.isEmpty()) list(store, out, err) else null
                "save" -> if (rest.size >= 2) save(store, rest, args.flag("replace"), out, err) else null
                "recall" -> if (rest.size == 2) recall(store, rest[0], rest[1], out, err) else null
                "rename" -> if (rest.size == 2) rename(store, rest[0], rest[1], out, err) else null
                "move" -> if (rest.size == 2) move(store, rest[0], rest[1], out, err) else null
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
        list.tones.forEachIndexed { index, tone -> out.println(Json.line("position" to index + 1, *fields(tone))) }
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
        val amp =
            AmpArgument.parse(rest[0]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
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
                    .getOrElse {
                        err.println(it.message)
                        return ExitCodes.BAD_ARGUMENTS
                    }.value
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
        val target = WmiAddress.parse(address)
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        val link =
            WmiLink.open(target.host, target.port).getOrElse {
                err.println("cannot reach $target: ${it.message}")
                return ExitCodes.NO_LINK
            }
        val messages = tone.recall()
        link.use {
            messages.forEach { message ->
                it.send(message).getOrElse { failure ->
                    err.println("send failed: ${failure.message}")
                    return ExitCodes.NO_LINK
                }
            }
        }
        out.println(
            Json.line(
                "host" to target.host,
                "port" to target.port,
                "tone" to tone.name,
                "amp" to tone.amp.id,
                "sent" to messages.size,
            ),
        )
        return ExitCodes.OK
    }

    private fun rename(
        store: ToneStore,
        name: String,
        to: String,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val tone = found(store, name, err) ?: return ExitCodes.NOTHING_FOUND
        return changed(store.rename(tone.name, to), out, err, "renamed" to tone.name, "to" to to)
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
        val text =
            try {
                file.readText()
            } catch (e: IOException) {
                err.println("cannot read $file: ${e.message}")
                return ExitCodes.BAD_FILE
            }
        val read =
            ToneFile.read(text).getOrElse {
                err.println("$file: ${it.message}")
                return ExitCodes.BAD_FILE
            }
        read.skipped.forEach { err.println("$file: skipped: $it") }
        return changed(
            store.save(read.tone, replace),
            out,
            err,
            "imported" to read.tone.name,
            "skipped" to read.skipped,
        )
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
}
