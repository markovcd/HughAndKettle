package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The [Song] of each amp model, kept in one folder as `<amp-id>.json`, version 1:
 *
 * ```json
 * { "version": 1, "amp": "grandmeister_deluxe_40", "tempo": 120, "steps": [ { "preset": 5, "beats": 32 } ] }
 * ```
 *
 * The keys are persisted: never renamed. A missing file is a song with no steps; a damaged or newer one
 * is a failure, and is never written over. Every file is written beside itself and moved into place.
 */
public class SongStore(
    private val folder: Path,
) {
    private val json = Json { prettyPrint = true }

    public fun song(amp: AmpModel): Result<Song> {
        val file = fileOf(amp)
        val text =
            try {
                if (!file.exists()) return Result.success(Song(amp))
                file.readText()
            } catch (e: IOException) {
                return Result.failure(IOException("cannot read $file: ${e.message}", e))
            }
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
            val temporary = folder.resolve(".${song.amp.id}.json.tmp")
            temporary.writeText(write(song))
            Files.move(temporary, fileOf(song.amp), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write a song in $folder: ${e.message}", e))
        }
    }

    private fun fileOf(amp: AmpModel): Path = folder.resolve("${amp.id}.json")

    private fun write(song: Song): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("version", VERSION)
                put("amp", song.amp.id)
                put("tempo", song.tempo)
                putJsonArray("steps") {
                    song.steps.forEach {
                        addJsonObject {
                            put("preset", it.preset)
                            put("beats", it.beats)
                        }
                    }
                }
            },
        )

    private fun read(
        amp: AmpModel,
        text: String,
    ): Result<Song> {
        val root =
            try {
                Json.parseToJsonElement(text) as? JsonObject
            } catch (e: SerializationException) {
                return failure("is not JSON: ${e.message?.lineSequence()?.first()}")
            } ?: return failure("is not a JSON object")
        val version = root.int("version")
        when {
            version == null -> return failure("has no version")
            version > VERSION -> return failure("is version $version, from a newer Hugh & Kettle; this reads $VERSION")
            version < 1 -> return failure("has version $version; versions start at 1")
        }
        val ampId = (root["amp"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (ampId != amp.id) return failure("is for amp '$ampId', not '${amp.id}'")
        val tempo = root.int("tempo")?.takeIf { it in Song.TEMPOS }
        if (tempo ==
            null
        ) {
            return failure("has tempo ${root["tempo"]}; a tempo is ${Song.TEMPOS.first}..${Song.TEMPOS.last}")
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
        return Result.success(Song(amp, tempo, kept))
    }

    private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

    private fun failure(why: String): Result<Song> = Result.failure(IllegalArgumentException(why))

    private companion object {
        const val VERSION = 1
    }
}
