package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The [Song] of each amp model, kept in one folder as `<amp-id>.json`, version 1:
 *
 * ```json
 * { "version": 1, "amp": "grandmeister_deluxe_40", "tempo": 120, "count_in": true, "steps": [ { "preset": 5, "beats": 32 } ] }
 * ```
 *
 * A file with no `count_in` counts in; an older reader passes over the key.
 * The keys are persisted: never renamed. A missing file is a song with no steps; a damaged or newer one
 * is a failure, and is never written over. Every file is written beside itself and moved into place.
 */
public class SongStore(
    private val folder: Path,
) {
    public fun song(amp: AmpModel): Result<Song> {
        val file = fileOf(amp)
        val text = JsonFile.text(file).getOrElse { return Result.failure(it) } ?: return Result.success(Song(amp))
        return read(amp, text).fold(
            { Result.success(it) },
            { Result.failure(IllegalArgumentException("$file ${it.message}", it)) },
        )
    }

    /** Keeps [song] as its amp's, unless the one kept now cannot be read. */
    public fun save(song: Song): Result<Unit> {
        song(song.amp).onFailure { return Result.failure(it) }
        return try {
            Files.createDirectories(folder)
            JsonFile.place(fileOf(song.amp), write(song))
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write a song in $folder: ${e.message}", e))
        }
    }

    private fun fileOf(amp: AmpModel): Path = folder.resolve("${amp.id}.json")

    private fun write(song: Song): String =
        JsonFile.write(VERSION) {
            put("amp", song.amp.id)
            put("tempo", song.tempo)
            put("count_in", song.countIn)
            putJsonArray("steps") {
                song.steps.forEach {
                    addJsonObject {
                        put("preset", it.preset)
                        put("beats", it.beats)
                    }
                }
            }
        }

    private fun read(
        amp: AmpModel,
        text: String,
    ): Result<Song> {
        val root = JsonFile.read(text, VERSION).getOrElse { return Result.failure(it) }
        val ampId = JsonFile.string(root, "amp")
        if (ampId != amp.id) return failure("is for amp '$ampId', not '${amp.id}'")
        val tempo = root.int("tempo")?.takeIf { it in Song.TEMPOS }
        if (tempo ==
            null
        ) {
            return failure("has tempo ${root["tempo"]}; a tempo is ${Song.TEMPOS.first}..${Song.TEMPOS.last}")
        }
        val countIn =
            when (val given = root["count_in"]) {
                null -> {
                    true
                }

                else -> {
                    (given as? JsonPrimitive)?.takeUnless { it.isString }?.booleanOrNull
                        ?: return failure("has count_in $given; it is true or false")
                }
            }
        val steps = root["steps"] as? JsonArray ?: return failure("has no steps")
        val kept =
            steps.mapIndexed { index, element ->
                val step = element as? JsonObject
                val preset = step?.int("preset")?.takeIf { it in 1..amp.presetCount }
                val beats = step?.int("beats")?.takeIf { it in 1..SongStep.MOST_BEATS }
                if (preset ==
                    null
                ) {
                    return failure(
                        "step ${index + 1} has preset ${step?.get("preset")}; ${amp.name} has 1..${amp.presetCount}",
                    )
                }
                if (beats ==
                    null
                ) {
                    return failure(
                        "step ${index + 1} lasts ${step["beats"]} beats; a step lasts 1..${SongStep.MOST_BEATS}",
                    )
                }
                SongStep(preset, beats)
            }
        return Result.success(Song(amp, tempo, kept, countIn))
    }

    private fun JsonObject.int(key: String): Int? = JsonFile.int(this, key)

    private fun failure(why: String): Result<Song> = Result.failure(IllegalArgumentException(why))

    private companion object {
        const val VERSION = 1
    }
}
