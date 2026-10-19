package io.github.markovcd.hughandkettle.amp

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText

/**
 * The songs of each amp model, a folder `<amp-id>` apiece: a file per song, `<n>.json` in [SongFile]'s
 * format, an `order` file naming them in the order they are shown under a version line, and an `open`
 * file naming the open one. A song missing from `order` comes after the rest, by name. Names are unique
 * for an amp regardless of case. A file that cannot be read is listed with its problem and never written
 * over. Every file is written beside itself and moved into place.
 *
 * The single song an older Hugh & Kettle kept as `<amp-id>.json` is carried into the amp's folder as
 * [CARRIED] the first time the folder is missing, and left where it is for that older reader.
 */
public class SongStore(
    private val folder: Path,
) {
    private data class Entry(
        val file: String,
        val kept: KeptSong,
    )

    public fun list(amp: AmpModel): Result<SongList> {
        val carried =
            try {
                carry(amp)
            } catch (e: IOException) {
                return Result.failure(
                    IOException("cannot carry over ${folder.resolve("${amp.id}.json")}: ${e.message}", e),
                )
            }
        val entries = read(amp).getOrElse { return Result.failure(it) }
        val open = readLines(amp, OPEN).firstOrNull()
        return Result.success(
            SongList(entries.map { it.kept }, entries.firstOrNull { it.file == open }?.kept?.name, carried),
        )
    }

    /**
     * Saves [song] at the end of its amp's list, or in place of the song of the same name when [replace] is
     * set. A song whose file cannot be read is not replaced.
     */
    public fun save(
        song: Song,
        replace: Boolean = false,
    ): Result<Unit> =
        change(song.amp) { entries ->
            val index = entries.indexOfName(song.name)
            val kept = KeptSong(song.name, song, null)
            when {
                index < 0 -> {
                    Result.success(entries + Entry(nextFile(song.amp, entries), kept))
                }

                entries[index].kept.song == null -> {
                    refused(
                        "'${entries[index].kept.name}' cannot be read, so it is not written over",
                    )
                }

                replace -> {
                    Result.success(entries.replaced(index, kept))
                }

                else -> {
                    refused("a song named '${entries[index].kept.name}' exists")
                }
            }
        }

    public fun rename(
        amp: AmpModel,
        name: String,
        to: String,
    ): Result<Unit> =
        change(amp) { entries ->
            val index = entries.indexOfName(name)
            val taken = entries.indexOfName(to)
            val song = entries.getOrNull(index)?.kept?.song
            when {
                index < 0 -> refused("there is no song named '$name'")
                song == null -> refused("'$name' cannot be read, so it cannot be renamed")
                to.isBlank() -> refused("a song needs a name")
                taken >= 0 && taken != index -> refused("a song named '${entries[taken].kept.name}' exists")
                else -> Result.success(entries.replaced(index, KeptSong(to, song.copy(name = to), null)))
            }
        }

    /** Deletes the song [name], read or not. */
    public fun delete(
        amp: AmpModel,
        name: String,
    ): Result<Unit> =
        change(amp) { entries ->
            val index = entries.indexOfName(name)
            if (index < 0) refused("there is no song named '$name'") else Result.success(entries - entries[index])
        }

    /** Moves the song [name] to [position], counted from 1. */
    public fun move(
        amp: AmpModel,
        name: String,
        position: Int,
    ): Result<Unit> =
        change(amp) { entries ->
            val index = entries.indexOfName(name)
            when {
                index < 0 -> refused("there is no song named '$name'")
                position !in 1..entries.size -> refused("there are positions 1..${entries.size}, not $position")
                else -> Result.success(entries.toMutableList().apply { add(position - 1, removeAt(index)) })
            }
        }

    /** Marks the song [name] as the one open. */
    public fun open(
        amp: AmpModel,
        name: String,
    ): Result<Unit> {
        val entries = read(amp).getOrElse { return Result.failure(it) }
        val entry =
            entries.getOrNull(entries.indexOfName(name))
                ?: return Result.failure(IllegalArgumentException("there is no song named '$name'"))
        return writing(amp) { place(amp, OPEN, "${entry.file}\n") }
    }

    /** Reads the amp's folder, applies [edit], and writes back the files whose song changed, then the order. */
    private fun change(
        amp: AmpModel,
        edit: (List<Entry>) -> Result<List<Entry>>,
    ): Result<Unit> {
        val before = read(amp).getOrElse { return Result.failure(it) }
        val after = edit(before).getOrElse { return Result.failure(it) }
        return writing(amp) {
            after.filter { it !in before }.forEach {
                place(
                    amp,
                    it.file,
                    SongFile.write(it.kept.song ?: return@forEach),
                )
            }
            place(amp, ORDER, (listOf("$ORDER_VERSION") + after.map { it.file }).joinToString("\n", postfix = "\n"))
            (before.map { it.file } - after.map { it.file }.toSet()).forEach {
                Files.deleteIfExists(
                    folderOf(amp).resolve(it),
                )
            }
        }
    }

    private fun writing(
        amp: AmpModel,
        write: () -> Unit,
    ): Result<Unit> =
        try {
            Files.createDirectories(folderOf(amp))
            write()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write songs in ${folderOf(amp)}: ${e.message}", e))
        }

    /** The amp's songs in their order; a file that cannot be read is named by its `name`, else by its number. */
    private fun read(amp: AmpModel): Result<List<Entry>> {
        val files =
            jsonFiles(amp).getOrElse {
                return Result.failure(IOException("cannot list ${folderOf(amp)}: ${it.message}", it))
            }
        val entries =
            files.map { file ->
                val text =
                    try {
                        folderOf(amp).resolve(file).readText()
                    } catch (e: IOException) {
                        return@map Entry(file, KeptSong(fallbackName(file), null, "$file: ${e.message}"))
                    }
                val song =
                    SongFile.read(text).fold(
                        {
                            if (it.amp ==
                                amp
                            ) {
                                Result.success(it)
                            } else {
                                refusedSong("is for amp '${it.amp.id}', not '${amp.id}'")
                            }
                        },
                        { Result.failure(it) },
                    )
                song.fold(
                    { Entry(file, KeptSong(it.name, it, null)) },
                    { Entry(file, KeptSong(SongFile.nameOf(text) ?: fallbackName(file), null, "$file ${it.message}")) },
                )
            }
        val order = readLines(amp, ORDER).takeIf { it.firstOrNull() == "$ORDER_VERSION" }?.drop(1).orEmpty()
        return Result.success(
            entries.sortedWith(
                compareBy<Entry> { order.indexOf(it.file).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.kept.name }
                    .thenBy { it.file },
            ),
        )
    }

    /** Carries the older single song of [amp] into its folder, when there is no folder yet, and returns its name. */
    private fun carry(amp: AmpModel): String? {
        val old = folder.resolve("${amp.id}.json")
        if (folderOf(amp).exists() || !old.exists()) return null
        val text = old.readText()
        val song = SongFile.read(text).getOrNull()?.takeIf { it.amp == amp }
        Files.createDirectories(folderOf(amp))
        place(amp, FIRST, song?.let { SongFile.write(it.copy(name = CARRIED)) } ?: text)
        place(amp, ORDER, "$ORDER_VERSION\n$FIRST\n")
        place(amp, OPEN, "$FIRST\n")
        return CARRIED
    }

    /** The trimmed, non-empty lines of [file] in the amp's folder; none when it is missing or cannot be read. */
    private fun readLines(
        amp: AmpModel,
        file: String,
    ): List<String> =
        try {
            folderOf(
                amp,
            ).resolve(file).takeIf { it.exists() }?.readLines()?.map { it.trim() }?.filter { it.isNotEmpty() }.orEmpty()
        } catch (_: IOException) {
            emptyList()
        }

    private fun place(
        amp: AmpModel,
        file: String,
        text: String,
    ) = JsonFile.place(folderOf(amp).resolve(file), text)

    private fun folderOf(amp: AmpModel): Path = folder.resolve(amp.id)

    private fun nextFile(
        amp: AmpModel,
        entries: List<Entry>,
    ): String {
        val taken = entries.map { it.file }.toSet() + jsonFiles(amp).getOrDefault(emptyList())
        return generateSequence(1) { it + 1 }.map { "$it.json" }.first { it !in taken }
    }

    /** Every song file in the amp's folder, read or not; none while there is no folder. */
    private fun jsonFiles(amp: AmpModel): Result<List<String>> =
        try {
            val at = folderOf(amp)
            Result.success(
                if (at.exists()) {
                    at
                        .listDirectoryEntries("*.json")
                        .filter {
                            it.isRegularFile()
                        }.map { it.name }
                } else {
                    emptyList()
                },
            )
        } catch (e: IOException) {
            Result.failure(e)
        }

    private fun List<Entry>.indexOfName(name: String): Int =
        indexOfFirst { it.kept.name.equals(name, ignoreCase = true) }

    private fun List<Entry>.replaced(
        index: Int,
        kept: KeptSong,
    ): List<Entry> = toMutableList().apply { this[index] = Entry(this[index].file, kept) }

    private fun fallbackName(file: String): String = "Song ${file.removeSuffix(".json")}"

    private fun refused(why: String): Result<List<Entry>> = Result.failure(IllegalArgumentException(why))

    private fun refusedSong(why: String): Result<Song> = Result.failure(IllegalArgumentException(why))

    public companion object {
        /** The name the older single song of an amp is carried over under. */
        public const val CARRIED: String = "Song 1"

        private const val FIRST = "1.json"
        private const val ORDER = "order"
        private const val ORDER_VERSION = 1
        private const val OPEN = "open"
    }
}
