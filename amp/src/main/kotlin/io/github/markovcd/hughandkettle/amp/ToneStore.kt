package io.github.markovcd.hughandkettle.amp

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readLines
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The tones kept in one folder: a file per tone, `<n>.json` in [ToneFile]'s format, and an `order`
 * file naming them in the order they are shown, under a version line. A tone missing from `order`
 * comes after the rest, by name. Names are unique regardless of case. Every file is written beside
 * itself and moved into place, so a crash leaves the old file or the new one.
 */
public class ToneStore(
    private val folder: Path,
) {
    private data class Entry(
        val file: String,
        val tone: Tone,
    )

    public fun list(): ToneList {
        val (entries, problems) = read()
        return ToneList(entries.map { it.tone }, problems)
    }

    public fun find(name: String): Tone? = read().first.let { it.getOrNull(it.indexOfName(name)) }?.tone

    /** Saves [tone] at the end of the list, or in place of the tone of the same name when [replace] is set. */
    public fun save(
        tone: Tone,
        replace: Boolean = false,
    ): Result<Unit> = save(listOf(tone), replace)

    /** Saves [tones] in one change, each as [save] would; one refused refuses them all. */
    public fun save(
        tones: List<Tone>,
        replace: Boolean = false,
    ): Result<Unit> =
        change { before ->
            tones.fold(Result.success(before)) { result, tone ->
                val entries = result.getOrElse { return@fold result }
                val index = entries.indexOfName(tone.name)
                when {
                    index < 0 -> Result.success(entries + Entry(nextFile(entries), tone))
                    replace -> Result.success(entries.replaced(index, tone))
                    else -> refused("a tone named '${entries[index].tone.name}' exists")
                }
            }
        }

    public fun rename(
        name: String,
        to: String,
    ): Result<Unit> =
        change { entries ->
            val index = entries.indexOfName(name)
            val taken = entries.indexOfName(to)
            when {
                index < 0 -> refused("there is no tone named '$name'")
                to.isBlank() -> refused("a tone needs a name")
                taken >= 0 && taken != index -> refused("a tone named '${entries[taken].tone.name}' exists")
                else -> Result.success(entries.replaced(index, entries[index].tone.copy(name = to)))
            }
        }

    public fun delete(name: String): Result<Unit> =
        change { entries ->
            val index = entries.indexOfName(name)
            if (index < 0) refused("there is no tone named '$name'") else Result.success(entries - entries[index])
        }

    /** Moves the tone [name] to [position], counted from 1. */
    public fun move(
        name: String,
        position: Int,
    ): Result<Unit> =
        change { entries ->
            val index = entries.indexOfName(name)
            when {
                index < 0 -> refused("there is no tone named '$name'")
                position !in 1..entries.size -> refused("there are positions 1..${entries.size}, not $position")
                else -> Result.success(entries.toMutableList().apply { add(position - 1, removeAt(index)) })
            }
        }

    /** Reads the folder, applies [edit], and writes back the files whose tone changed, then the order. */
    private fun change(edit: (List<Entry>) -> Result<List<Entry>>): Result<Unit> {
        val before = read().first
        val after = edit(before).getOrElse { return Result.failure(it) }
        return try {
            Files.createDirectories(folder)
            after.filter { it !in before }.forEach { write(it.file, ToneFile.write(it.tone)) }
            write(ORDER, (listOf("$ORDER_VERSION") + after.map { it.file }).joinToString("\n", postfix = "\n"))
            (before.map { it.file } - after.map { it.file }.toSet()).forEach {
                Files.deleteIfExists(
                    folder.resolve(it),
                )
            }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write tones in $folder: ${e.message}", e))
        }
    }

    private fun read(): Pair<List<Entry>, List<String>> {
        if (!folder.exists()) return emptyList<Entry>() to emptyList()
        val problems = ArrayList<String>()
        val files = jsonFiles().getOrElse { return emptyList<Entry>() to listOf("cannot list $folder: ${it.message}") }
        val entries =
            files.mapNotNull { file ->
                try {
                    ToneFile
                        .read(folder.resolve(file).readText())
                        .map { Entry(file, it.tone) }
                        .onFailure { problems += "$file: ${it.message}" }
                        .getOrNull()
                } catch (e: IOException) {
                    problems += "$file: ${e.message}"
                    null
                }
            }
        val order = readOrder()
        val sorted =
            entries.sortedWith(
                compareBy<Entry> { order.indexOf(it.file).let { i -> if (i < 0) Int.MAX_VALUE else i } }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.tone.name }
                    .thenBy { it.file },
            )
        return sorted to problems
    }

    /** The files `order` names; a missing, damaged or newer `order` names none, and the tones fall back to name order. */
    private fun readOrder(): List<String> {
        val lines =
            try {
                folder.resolve(ORDER).takeIf { it.exists() }?.readLines() ?: return emptyList()
            } catch (_: IOException) {
                return emptyList()
            }
        if (lines.firstOrNull()?.trim()?.toIntOrNull() != ORDER_VERSION) return emptyList()
        return lines.drop(1).map { it.trim() }.filter { it.isNotEmpty() }
    }

    private fun write(
        file: String,
        text: String,
    ) {
        val temporary = folder.resolve(".$file.tmp")
        temporary.writeText(text)
        Files.move(temporary, folder.resolve(file), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    private fun nextFile(entries: List<Entry>): String {
        val taken = entries.map { it.file }.toSet() + jsonFiles().getOrDefault(emptyList())
        return generateSequence(1) { it + 1 }.map { "$it.json" }.first { it !in taken }
    }

    /** Every tone file in the folder, read or not. */
    private fun jsonFiles(): Result<List<String>> =
        try {
            Result.success(folder.listDirectoryEntries("*.json").filter { it.isRegularFile() }.map { it.name })
        } catch (e: IOException) {
            Result.failure(e)
        }

    private fun List<Entry>.indexOfName(name: String): Int =
        indexOfFirst { it.tone.name.equals(name, ignoreCase = true) }

    private fun List<Entry>.replaced(
        index: Int,
        tone: Tone,
    ): List<Entry> = toMutableList().apply { this[index] = Entry(this[index].file, tone) }

    private fun refused(why: String): Result<List<Entry>> = Result.failure(IllegalArgumentException(why))

    private companion object {
        const val ORDER = "order"
        const val ORDER_VERSION = 1
    }
}
