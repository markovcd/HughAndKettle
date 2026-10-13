package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.PresetNameStore
import java.io.IOException
import java.io.PrintWriter
import kotlin.io.path.Path

/**
 * Names the amp's presets, the way the app names them on the phone, in a folder, `--dir` or
 * `~/.hk/preset-names`. The amp itself keeps no names; `hk tone read` names the tones it reads after them.
 */
public object PresetCommand : Command {
    override val name: String = "preset"
    override val usage: String =
        """
        hk preset list <amp-id> [--dir D]
        hk preset name <amp-id> <preset> <name> [--dir D]
        hk preset unname <amp-id> <preset> [--dir D]
        """.trimIndent()

    /** The folder of preset names [args] point at with [option]. */
    internal fun store(
        args: Options,
        option: String = "names",
    ): PresetNameStore =
        PresetNameStore(args.value(option)?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "preset-names"))

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val rest = args.positional.drop(1)
        val verb = args.positional.firstOrNull()
        val sizes = mapOf("list" to 1, "name" to 3, "unname" to 2)
        if (sizes[verb] != rest.size) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(rest[0]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val store = store(args, "dir")
        if (verb == "list") {
            val names =
                store.names(amp).getOrElse {
                    err.println(it.message)
                    return ExitCodes.BAD_FILE
                }
            names.names.toSortedMap().forEach { (preset, name) ->
                out.println(
                    Json.line(
                        "preset" to preset,
                        "name" to name,
                    ),
                )
            }
            return ExitCodes.OK
        }
        val preset = rest[1].toIntOrNull()
        if (preset == null) {
            err.println("the preset must be a whole number, got '${rest[1]}'")
            return ExitCodes.BAD_ARGUMENTS
        }
        val name = rest.getOrElse(2) { "" }
        if (verb == "name" && name.isBlank()) {
            err.println("a preset name cannot be blank; hk preset unname takes one away")
            return ExitCodes.BAD_ARGUMENTS
        }
        store.name(amp, preset, name).onFailure {
            err.println(it.message)
            return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
        }
        out.println(Json.line("amp" to amp.id, "preset" to preset, "name" to name.trim().ifEmpty { null }))
        return ExitCodes.OK
    }
}
