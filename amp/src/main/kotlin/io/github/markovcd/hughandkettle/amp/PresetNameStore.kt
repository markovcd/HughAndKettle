package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The [PresetNames] of each amp model, kept in one folder as `<amp-id>.json`, version 1:
 *
 * ```json
 * { "version": 1, "amp": "grandmeister_deluxe_40", "names": { "12": "Lead solo" } }
 * ```
 *
 * The keys are persisted: never renamed. A missing file names nothing; a damaged or newer one is a
 * failure, and is never written over. Every file is written beside itself and moved into place.
 */
public class PresetNameStore(
    private val folder: Path,
) {
    private val json = Json { prettyPrint = true }

    public fun names(amp: AmpModel): Result<PresetNames> {
        val file = fileOf(amp)
        val text =
            try {
                if (!file.exists()) return Result.success(PresetNames(amp))
                file.readText()
            } catch (e: IOException) {
                return Result.failure(IOException("cannot read $file: ${e.message}", e))
            }
        return read(amp, text).fold(
            { Result.success(it) },
            { Result.failure(IllegalArgumentException("$file ${it.message}", it)) },
        )
    }

    /** Names [preset] of [amp] [name], or takes its name away when [name] is blank. */
    public fun name(
        amp: AmpModel,
        preset: Int,
        name: String,
    ): Result<Unit> {
        if (preset !in 1..amp.presetCount) {
            return Result.failure(
                IllegalArgumentException("${amp.name} has presets 1..${amp.presetCount}, not $preset"),
            )
        }
        val after = names(amp).getOrElse { return Result.failure(it) }.with(preset, name)
        return try {
            Files.createDirectories(folder)
            val temporary = folder.resolve(".${amp.id}.json.tmp")
            temporary.writeText(write(after))
            Files.move(temporary, fileOf(amp), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write preset names in $folder: ${e.message}", e))
        }
    }

    private fun fileOf(amp: AmpModel): Path = folder.resolve("${amp.id}.json")

    private fun write(names: PresetNames): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("version", VERSION)
                put("amp", names.amp.id)
                putJsonObject("names") { names.names.toSortedMap().forEach { (preset, name) -> put("$preset", name) } }
            },
        )

    private fun read(
        amp: AmpModel,
        text: String,
    ): Result<PresetNames> {
        val root =
            try {
                Json.parseToJsonElement(text) as? JsonObject
            } catch (e: SerializationException) {
                return failure("is not JSON: ${e.message?.lineSequence()?.first()}")
            } ?: return failure("is not a JSON object")
        val version = (root["version"] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
        when {
            version == null -> return failure("has no version")
            version > VERSION -> return failure("is version $version, from a newer Hugh & Kettle; this reads $VERSION")
            version < 1 -> return failure("has version $version; versions start at 1")
        }
        val ampId = (root["amp"] as? JsonPrimitive)?.takeIf { it.isString }?.content
        if (ampId != amp.id) return failure("is for amp '$ampId', not '${amp.id}'")
        val names = root["names"] as? JsonObject ?: return failure("has no names")
        val kept = LinkedHashMap<Int, String>()
        for ((key, element) in names) {
            val preset = key.toIntOrNull()?.takeIf { it in 1..amp.presetCount }
            val name = (element as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }
            if (preset == null) return failure("names preset '$key'; ${amp.name} has 1..${amp.presetCount}")
            if (name == null) return failure("gives preset $preset the name $element")
            kept[preset] = name
        }
        return Result.success(PresetNames(amp, kept))
    }

    private fun failure(why: String): Result<PresetNames> = Result.failure(IllegalArgumentException(why))

    private companion object {
        const val VERSION = 1
    }
}
