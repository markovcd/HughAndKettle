package io.github.markovcd.hughandkettle.amp

import java.nio.file.Path

/**
 * The tones kept in one folder: a file per tone, `<n>.json` in [ToneFile]'s format, and an `order`
 * file naming them in the order they are shown, under a version line. A tone missing from `order`
 * comes after the rest, by name. Names are unique regardless of case. Every file is written beside
 * itself and moved into place, so a crash leaves the old file or the new one.
 */
public class ToneStore(
    folder: Path,
) {
    private val files =
        OrderedFolder<Tone>(
            folder,
            what = "tone",
            nameOf = { it.name },
            text = ToneFile::write,
            parse = { _, text -> text.mapCatching { ToneFile.read(it).getOrThrow().tone } },
        )

    public fun list(): ToneList =
        files.read().fold(
            { contents -> ToneList(contents.entries.map { it.item }, contents.problems) },
            { ToneList(emptyList(), listOf("${it.message}")) },
        )

    public fun find(name: String): Tone? {
        val entries = files.read().getOrNull()?.entries ?: return null
        return entries.getOrNull(files.indexOf(entries, name))?.item
    }

    /** Saves [tone] at the end of the list, or in place of the tone of the same name, keeping its pin, when [replace] is set. */
    public fun save(
        tone: Tone,
        replace: Boolean = false,
    ): Result<Unit> = save(listOf(tone), replace)

    /** Saves [tones] in one change, each as [save] would; one refused refuses them all. */
    public fun save(
        tones: List<Tone>,
        replace: Boolean = false,
    ): Result<Unit> =
        files.change { before ->
            tones.fold(Result.success(before)) { result, tone ->
                val entries = result.getOrElse { return@fold result }
                val index = files.indexOf(entries, tone.name)
                when {
                    index < 0 -> {
                        Result.success(files.added(entries, tone))
                    }

                    replace -> {
                        val pinned = entries[index].item.pinned
                        Result.success(files.replaced(entries, index, tone.copy(pinned = pinned)))
                    }

                    else -> {
                        files.refused("a tone named '${entries[index].item.name}' exists")
                    }
                }
            }
        }

    public fun rename(
        name: String,
        to: String,
    ): Result<Unit> =
        files.change { entries ->
            val index = files.indexOf(entries, name)
            val refusal = files.renameRefusal(entries, index, to)
            when {
                index < 0 -> files.missing(name)
                refusal != null -> files.refused(refusal)
                else -> Result.success(files.replaced(entries, index, entries[index].item.copy(name = to)))
            }
        }

    /** Pins the tone [name] to the header, or unpins it. */
    public fun pin(
        name: String,
        on: Boolean,
    ): Result<Unit> =
        files.change { entries ->
            val index = files.indexOf(entries, name)
            if (index < 0) {
                files.missing(name)
            } else {
                Result.success(files.replaced(entries, index, entries[index].item.copy(pinned = on)))
            }
        }

    public fun delete(name: String): Result<Unit> = files.change { files.deleted(it, name) }

    /** Moves the tone [name] to [position], counted from 1. */
    public fun move(
        name: String,
        position: Int,
    ): Result<Unit> = files.change { files.moved(it, name, position) }
}
