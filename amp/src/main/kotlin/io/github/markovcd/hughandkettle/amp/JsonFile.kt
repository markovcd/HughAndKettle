package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * What every file the stores keep shares: a JSON object stamped with a `version`, a newer version
 * refused, and each write made beside the file and moved into place, so a crash leaves the old file or
 * the new one.
 */
internal object JsonFile {
    private val json = Json { prettyPrint = true }

    /** The text of [fields] under a `version` of [version]. */
    fun write(
        version: Int,
        fields: JsonObjectBuilder.() -> Unit,
    ): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("version", version)
                fields()
            },
        )

    /**
     * The object in [text], if it is one with a version from 1 to [version]. A failure's message follows
     * the file's name, as in "is not JSON".
     */
    fun read(
        text: String,
        version: Int,
    ): Result<JsonObject> {
        val root =
            try {
                Json.parseToJsonElement(text) as? JsonObject
            } catch (e: SerializationException) {
                return failure("is not JSON: ${e.message?.lineSequence()?.first()}")
            } ?: return failure("is not a JSON object")
        val found = int(root, "version")
        return when {
            found == null -> failure("has no version")
            found > version -> failure("is version $found, from a newer Hugh & Kettle; this reads $version")
            found < 1 -> failure("has version $found; versions start at 1")
            else -> Result.success(root)
        }
    }

    /** The text of [file], or null when there is none. */
    fun text(file: Path): Result<String?> =
        try {
            Result.success(if (file.exists()) file.readText() else null)
        } catch (e: IOException) {
            Result.failure(IOException("cannot read $file: ${e.message}", e))
        }

    /** Writes [text] to [file] beside it, then moves it into place. */
    @Throws(IOException::class)
    fun place(
        file: Path,
        text: String,
    ) {
        val temporary = file.resolveSibling(".${file.fileName}.tmp")
        temporary.writeText(text)
        Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    }

    /** [key]'s whole number; null when it is missing, a string or not whole. */
    fun int(
        root: JsonObject,
        key: String,
    ): Int? = (root[key] as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull

    /** [key]'s string; null when it is missing or not a string. */
    fun string(
        root: JsonObject,
        key: String,
    ): String? = (root[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun failure(why: String): Result<JsonObject> = Result.failure(IllegalArgumentException(why))
}
