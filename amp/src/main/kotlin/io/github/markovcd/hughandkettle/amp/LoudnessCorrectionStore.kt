package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * The [LoudnessCorrections] of each amp model, kept in one folder as `<amp-id>.json`, version 1:
 *
 * ```json
 * { "version": 1, "amp": "grandmeister_deluxe_40", "corrections": { "12": -11 } }
 * ```
 *
 * The keys are persisted: never renamed. A missing file corrects nothing; a damaged or newer one is a
 * failure, and is never written over. Every file is written beside itself and moved into place.
 */
public class LoudnessCorrectionStore(
    private val folder: Path,
) {
    public fun corrections(amp: AmpModel): Result<LoudnessCorrections> {
        val file = fileOf(amp)
        val text =
            JsonFile.text(file).getOrElse { return Result.failure(it) }
                ?: return Result.success(LoudnessCorrections(amp))
        return read(amp, text).fold(
            { Result.success(it) },
            { Result.failure(IllegalArgumentException("$file ${it.message}", it)) },
        )
    }

    /** Keeps [corrections] for their amp in place of the ones kept before. */
    public fun keep(corrections: LoudnessCorrections): Result<Unit> {
        corrections(corrections.amp).onFailure { return Result.failure(it) }
        return try {
            Files.createDirectories(folder)
            JsonFile.place(fileOf(corrections.amp), write(corrections))
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(IOException("cannot write loudness corrections in $folder: ${e.message}", e))
        }
    }

    private fun fileOf(amp: AmpModel): Path = folder.resolve("${amp.id}.json")

    private fun write(corrections: LoudnessCorrections): String =
        JsonFile.write(VERSION) {
            put("amp", corrections.amp.id)
            putJsonObject("corrections") {
                corrections.amounts.toSortedMap().forEach { (preset, amount) -> put("$preset", amount) }
            }
        }

    private fun read(
        amp: AmpModel,
        text: String,
    ): Result<LoudnessCorrections> {
        val root = JsonFile.read(text, VERSION).getOrElse { return Result.failure(it) }
        val ampId = JsonFile.string(root, "amp")
        if (ampId != amp.id) return failure("is for amp '$ampId', not '${amp.id}'")
        val amounts = root["corrections"] as? JsonObject ?: return failure("has no corrections")
        val kept = LinkedHashMap<Int, Int>()
        for ((key, element) in amounts) {
            val preset = key.toIntOrNull()?.takeIf { it in 1..amp.presetCount }
            val amount = (element as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
            if (preset == null) return failure("corrects preset '$key'; ${amp.name} has 1..${amp.presetCount}")
            if (amount == null || amount !in -127..127 || amount == 0) {
                return failure("corrects preset $preset by $element; a correction is -127..127 and not 0")
            }
            kept[preset] = amount
        }
        return Result.success(LoudnessCorrections(amp, kept))
    }

    private fun failure(why: String): Result<LoudnessCorrections> = Result.failure(IllegalArgumentException(why))

    private companion object {
        const val VERSION = 1
    }
}
