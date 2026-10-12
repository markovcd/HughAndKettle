package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

/**
 * How a tone is written to a file, version 1:
 *
 * ```json
 * { "version": 1, "amp": "black_spirit_200", "name": "Rhythm", "values": { "channel": 47, "gain": 40 } }
 * ```
 *
 * Values are the raw 0..127 a control is sent, so a selector option renamed later still reads back.
 * The keys are persisted: never renamed.
 */
public object ToneFile {
    public const val VERSION: Int = 1

    private val json = Json { prettyPrint = true }

    public fun write(tone: Tone): String =
        json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("version", VERSION)
                put("amp", tone.amp.id)
                put("name", tone.name)
                putJsonObject("values") {
                    tone.amp.controls.forEach { control -> tone.values[control.id]?.let { put(control.id, it) } }
                }
            },
        )

    /**
     * The tone in [text]. A file from a newer version, or one without an amp, a name or values, is a
     * failure; a value for a control the amp lacks, or outside 0..127, is skipped and said in [ToneRead.skipped].
     */
    public fun read(text: String): Result<ToneRead> {
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
        val ampId = root.string("amp") ?: return failure("names no amp")
        val amp = AmpModels.byId(ampId) ?: return failure("is for amp '$ampId', which this version does not know")
        val name = root.string("name")?.takeIf { it.isNotBlank() } ?: return failure("has no name")
        val values = root["values"] as? JsonObject ?: return failure("has no values")
        val kept = LinkedHashMap<String, Int>()
        val skipped = ArrayList<String>()
        for ((id, element) in values) {
            val value = (element as? JsonPrimitive)?.takeUnless { it.isString }?.intOrNull
            when {
                amp.control(id) == null -> skipped += "${amp.name} has no control '$id'"
                value == null || value !in 0..127 -> skipped += "'$id' is $element, not 0..127"
                else -> kept[id] = value
            }
        }
        return Result.success(ToneRead(Tone(name, amp, kept), skipped))
    }

    private fun JsonObject.string(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    private fun failure(why: String): Result<ToneRead> = Result.failure(IllegalArgumentException("the tone file $why"))
}
