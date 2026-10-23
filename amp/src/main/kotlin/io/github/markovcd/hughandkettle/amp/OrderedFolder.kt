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
 * A folder of named items, one `<n>.json` file apiece, and an `order` file naming the files in the order
 * they are shown under a version line. A file missing from `order` comes after the rest, by name. Names are
 * unique regardless of case. Every file is written beside itself and moved into place.
 */
internal class OrderedFolder<T : Any>(
    private val folder: Path,
    /** What the items are called in a refusal, as in "there is no [what] named". */
    private val what: String,
    private val nameOf: (T) -> String,
    /** The text of an item's file; null for one that is never written, as a file that cannot be read. */
    private val text: (T) -> String?,
    /** The item in a file, from its name and its text or why the text could not be read; a failure leaves it out. */
    private val parse: (file: String, text: Result<String>) -> Result<T>,
) {
    data class Entry<T>(
        val file: String,
        val item: T,
    )

    /** The items in their order, and a sentence for each file left out. */
    data class Contents<T>(
        val entries: List<Entry<T>>,
        val problems: List<String>,
    )

    fun read(): Result<Contents<T>> {
        val files =
            jsonFiles().getOrElse { return Result.failure(IOException("cannot list $folder: ${it.message}", it)) }
        val problems = ArrayList<String>()
        val entries =
            files.mapNotNull { file ->
                val text =
                    try {
                        Result.success(folder.resolve(file).readText())
                    } catch (e: IOException) {
                        Result.failure(e)
                    }
                parse(file, text)
                    .onFailure { problems += "$file: ${it.message}" }
                    .getOrNull()
                    ?.let { Entry(file, it) }
            }
        val order = lines(ORDER).takeIf { it.firstOrNull() == "$ORDER_VERSION" }?.drop(1).orEmpty()
        val sorted =
            entries.sortedWith(
                compareBy<Entry<T>> { order.indexOf(it.file).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { nameOf(it.item) }
                    .thenBy { it.file },
            )
        return Result.success(Contents(sorted, problems))
    }

    /**
     * Reads the folder, applies [edit], and writes back the files whose item changed, then the order, then
     * removes the files no longer listed.
     */
    fun change(edit: (List<Entry<T>>) -> Result<List<Entry<T>>>): Result<Unit> {
        val before = read().getOrElse { return Result.failure(it) }.entries
        val after = edit(before).getOrElse { return Result.failure(it) }
        return writing {
            after.filter { it !in before }.forEach { entry -> text(entry.item)?.let { place(entry.file, it) } }
            place(ORDER, (listOf("$ORDER_VERSION") + after.map { it.file }).joinToString("\n", postfix = "\n"))
            (before.map { it.file } - after.map { it.file }.toSet()).forEach {
                Files.deleteIfExists(folder.resolve(it))
            }
        }
    }

    /** Runs [write] with the folder made, and says where it failed. */
    fun writing(write: () -> Unit): Result<Unit> =
        try {
            Files.createDirectories(folder)
            write()
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write ${what}s in $folder: ${e.message}", e))
        }

    /** [entries] with [item] at the end, in a file of its own. */
    fun added(
        entries: List<Entry<T>>,
        item: T,
    ): List<Entry<T>> {
        val taken = entries.map { it.file }.toSet() + jsonFiles().getOrDefault(emptyList())
        val file = generateSequence(1) { it + 1 }.map { "$it.json" }.first { it !in taken }
        return entries + Entry(file, item)
    }

    /** The place of the item called [name] in [entries], regardless of case; -1 when there is none. */
    fun indexOf(
        entries: List<Entry<T>>,
        name: String,
    ): Int = entries.indexOfFirst { nameOf(it.item).equals(name, ignoreCase = true) }

    /** [entries] with the item at [index] swapped for [item], in the same file. */
    fun replaced(
        entries: List<Entry<T>>,
        index: Int,
        item: T,
    ): List<Entry<T>> = entries.toMutableList().apply { this[index] = Entry(this[index].file, item) }

    /** [entries] without the item called [name]. */
    fun deleted(
        entries: List<Entry<T>>,
        name: String,
    ): Result<List<Entry<T>>> {
        val index = indexOf(entries, name)
        return if (index < 0) missing(name) else Result.success(entries - entries[index])
    }

    /** [entries] with the item called [name] moved to [position], counted from 1. */
    fun moved(
        entries: List<Entry<T>>,
        name: String,
        position: Int,
    ): Result<List<Entry<T>>> {
        val index = indexOf(entries, name)
        return when {
            index < 0 -> missing(name)
            position !in 1..entries.size -> refused("there are positions 1..${entries.size}, not $position")
            else -> Result.success(entries.toMutableList().apply { add(position - 1, removeAt(index)) })
        }
    }

    /**
     * Why [name] cannot become [to] for the item at [index]: blank, or another item's name. Null when it can.
     */
    fun renameRefusal(
        entries: List<Entry<T>>,
        index: Int,
        to: String,
    ): String? {
        val taken = indexOf(entries, to)
        return when {
            to.isBlank() -> "a $what needs a name"
            taken >= 0 && taken != index -> "a $what named '${nameOf(entries[taken].item)}' exists"
            else -> null
        }
    }

    fun missing(name: String): Result<List<Entry<T>>> = refused("there is no $what named '$name'")

    fun refused(why: String): Result<List<Entry<T>>> = Result.failure(IllegalArgumentException(why))

    /** The trimmed, non-empty lines of [file]; none when it is missing or cannot be read. */
    fun lines(file: String): List<String> =
        try {
            folder
                .resolve(file)
                .takeIf { it.exists() }
                ?.readLines()
                ?.map { it.trim() }
                ?.filter { it.isNotEmpty() }
                .orEmpty()
        } catch (_: IOException) {
            emptyList()
        }

    @Throws(IOException::class)
    fun place(
        file: String,
        text: String,
    ) = JsonFile.place(folder.resolve(file), text)

    /** Every `.json` file in the folder, read or not; none while there is no folder. */
    private fun jsonFiles(): Result<List<String>> =
        try {
            Result.success(
                if (folder.exists()) {
                    folder.listDirectoryEntries("*.json").filter { it.isRegularFile() }.map { it.name }
                } else {
                    emptyList()
                },
            )
        } catch (e: IOException) {
            Result.failure(e)
        }

    private companion object {
        const val ORDER = "order"
        const val ORDER_VERSION = 1
    }
}
