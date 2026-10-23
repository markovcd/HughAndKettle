package io.github.markovcd.hughandkettle.amp

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
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
    public fun list(amp: AmpModel): Result<SongList> {
        val carried =
            try {
                carry(amp)
            } catch (e: IOException) {
                return Result.failure(
                    IOException("cannot carry over ${folder.resolve("${amp.id}.json")}: ${e.message}", e),
                )
            }
        val songs = folderOf(amp)
        val entries = songs.read().getOrElse { return Result.failure(it) }.entries
        val open = songs.lines(OPEN).firstOrNull()
        return Result.success(
            SongList(entries.map { it.item }, entries.firstOrNull { it.file == open }?.item?.name, carried),
        )
    }

    /**
     * Saves [song] at the end of its amp's list, or in place of the song of the same name when [replace] is
     * set. A song whose file cannot be read is not replaced.
     */
    public fun save(
        song: Song,
        replace: Boolean = false,
    ): Result<Unit> {
        val songs = folderOf(song.amp)
        return songs.change { entries ->
            val index = songs.indexOf(entries, song.name)
            val kept = KeptSong(song.name, song, null)
            when {
                index < 0 -> {
                    Result.success(songs.added(entries, kept))
                }

                entries[index].item.song == null -> {
                    songs.refused(
                        "'${entries[index].item.name}' cannot be read, so it is not written over",
                    )
                }

                replace -> {
                    Result.success(songs.replaced(entries, index, kept))
                }

                else -> {
                    songs.refused("a song named '${entries[index].item.name}' exists")
                }
            }
        }
    }

    public fun rename(
        amp: AmpModel,
        name: String,
        to: String,
    ): Result<Unit> {
        val songs = folderOf(amp)
        return songs.change { entries ->
            val index = songs.indexOf(entries, name)
            val song = entries.getOrNull(index)?.item?.song
            val refusal = songs.renameRefusal(entries, index, to)
            when {
                index < 0 -> songs.missing(name)
                song == null -> songs.refused("'$name' cannot be read, so it cannot be renamed")
                refusal != null -> songs.refused(refusal)
                else -> Result.success(songs.replaced(entries, index, KeptSong(to, song.copy(name = to), null)))
            }
        }
    }

    /**
     * Renames the tone [name] to [to] in every song of [amp] that steps on it; a song that cannot be read is
     * left alone, and with no song stepping on it nothing is written.
     */
    public fun renameTone(
        amp: AmpModel,
        name: String,
        to: String,
    ): Result<Unit> {
        val songs = folderOf(amp)
        val entries = songs.read().getOrElse { return Result.failure(it) }.entries
        if (entries.none { entry ->
                entry.item.song
                    ?.steps
                    .orEmpty()
                    .any { it.isTone(name) }
            }
        ) {
            return Result.success(Unit)
        }
        return songs.change { entries ->
            Result.success(
                entries.map { entry ->
                    val song = entry.item.song
                    if (song == null || song.steps.none { it.isTone(name) }) return@map entry
                    val steps = song.steps.map { if (it.isTone(name)) it.copy(tone = to) else it }
                    OrderedFolder.Entry(entry.file, entry.item.copy(song = song.copy(steps = steps)))
                },
            )
        }
    }

    /** Deletes the song [name], read or not. */
    public fun delete(
        amp: AmpModel,
        name: String,
    ): Result<Unit> = folderOf(amp).let { songs -> songs.change { songs.deleted(it, name) } }

    /** Moves the song [name] to [position], counted from 1. */
    public fun move(
        amp: AmpModel,
        name: String,
        position: Int,
    ): Result<Unit> = folderOf(amp).let { songs -> songs.change { songs.moved(it, name, position) } }

    /** Marks the song [name] as the one open. */
    public fun open(
        amp: AmpModel,
        name: String,
    ): Result<Unit> {
        val songs = folderOf(amp)
        val entries = songs.read().getOrElse { return Result.failure(it) }.entries
        val entry =
            entries.getOrNull(songs.indexOf(entries, name))
                ?: return Result.failure(IllegalArgumentException("there is no song named '$name'"))
        return songs.writing { songs.place(OPEN, "${entry.file}\n") }
    }

    /** Carries the older single song of [amp] into its folder, when there is no folder yet, and returns its name. */
    private fun carry(amp: AmpModel): String? {
        val old = folder.resolve("${amp.id}.json")
        if (folder.resolve(amp.id).exists() || !old.exists()) return null
        val text = old.readText()
        val song = SongFile.read(text).getOrNull()?.takeIf { it.amp == amp }
        val songs = folderOf(amp)
        Files.createDirectories(folder.resolve(amp.id))
        songs.place(FIRST, song?.let { SongFile.write(it.copy(name = CARRIED)) } ?: text)
        songs.place(ORDER, "$ORDER_VERSION\n$FIRST\n")
        songs.place(OPEN, "$FIRST\n")
        return CARRIED
    }

    /**
     * The amp's folder of songs; a file that cannot be read is listed with its problem, named by its `name`, else
     * by its number.
     */
    private fun folderOf(amp: AmpModel): OrderedFolder<KeptSong> =
        OrderedFolder(
            folder.resolve(amp.id),
            what = "song",
            nameOf = { it.name },
            text = { kept -> kept.song?.let(SongFile::write) },
            parse = { file, read ->
                Result.success(
                    read.fold(
                        { text ->
                            SongFile.read(text).fold(
                                { song ->
                                    if (song.amp == amp) {
                                        KeptSong(song.name, song, null)
                                    } else {
                                        val why = "$file is for amp '${song.amp.id}', not '${amp.id}'"
                                        unreadable(file, SongFile.nameOf(text), why)
                                    }
                                },
                                { unreadable(file, SongFile.nameOf(text), "$file ${it.message}") },
                            )
                        },
                        { unreadable(file, null, "$file: ${it.message}") },
                    ),
                )
            },
        )

    private fun unreadable(
        file: String,
        name: String?,
        problem: String,
    ): KeptSong = KeptSong(name ?: "Song ${file.removeSuffix(".json")}", null, problem)

    public companion object {
        /** The name the older single song of an amp is carried over under. */
        public const val CARRIED: String = "Song 1"

        private const val FIRST = "1.json"
        private const val ORDER = "order"
        private const val ORDER_VERSION = 1
        private const val OPEN = "open"
    }
}
