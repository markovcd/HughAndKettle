package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

/**
 * A [Song] as a file, version 1, kept on the phone and exported as it is:
 *
 * ```json
 * { "version": 1, "amp": "grandmeister_deluxe_40", "name": "Highway Star", "tempo": 120, "count_in": true, "steps": [ { "preset": 5, "beats": 32 }, { "tone": "Lead solo", "beats": 16 } ] }
 * ```
 *
 * A step names a preset or a tone. An older reader refuses a song with a tone step as a step with no preset.
 * A file with no `count_in` counts in, and one with no `name` is untitled; an older reader passes over both.
 * The keys are persisted: never renamed.
 */
public object SongFile {
    private const val VERSION = 1

    public fun write(song: Song): String =
        JsonFile.write(VERSION) {
            put("amp", song.amp.id)
            put("name", song.name)
            put("tempo", song.tempo)
            put("count_in", song.countIn)
            putJsonArray("steps") {
                song.steps.forEach {
                    addJsonObject {
                        it.preset?.let { preset -> put("preset", preset) }
                        it.tone?.let { tone -> put("tone", tone) }
                        put("beats", it.beats)
                    }
                }
            }
        }

    /** The id of the amp model [text] is for, read even when the rest of it cannot be; null when it names none. */
    public fun ampOf(text: String): String? =
        JsonFile.read(text, Int.MAX_VALUE).getOrNull()?.let { JsonFile.string(it, "amp") }

    /** The name [text] gives its song, read even when the rest of it cannot be; null when it gives none. */
    internal fun nameOf(text: String): String? =
        JsonFile.read(text, Int.MAX_VALUE).getOrNull()?.let { JsonFile.string(it, "name") }

    /** The song in [text]; a failure's message follows the file's name, as in "has tempo 900". */
    public fun read(text: String): Result<Song> {
        val root = JsonFile.read(text, VERSION).getOrElse { return Result.failure(it) }
        val ampId = JsonFile.string(root, "amp")
        val amp =
            ampId?.let(AmpModels::byId) ?: return failure("is for amp '$ampId', which Hugh & Kettle does not know")
        val name =
            when (val given = root["name"]) {
                null -> {
                    ""
                }

                else -> {
                    (given as? JsonPrimitive)?.takeIf { it.isString }?.content
                        ?: return failure("has name $given; it is text")
                }
            }
        val tempo = root.int("tempo")?.takeIf { it in Song.TEMPOS }
        if (tempo == null) {
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
                val tone = step?.get("tone")
                val beats = step?.int("beats")?.takeIf { it in 1..SongStep.MOST_BEATS }
                if (tone != null) {
                    val name =
                        (tone as? JsonPrimitive)?.takeIf { it.isString && it.content.isNotBlank() }?.content
                            ?: return failure("step ${index + 1} has tone $tone; it is a name")
                    if (beats == null) {
                        return failure(
                            "step ${index + 1} lasts ${step["beats"]} beats; a step lasts 1..${SongStep.MOST_BEATS}",
                        )
                    }
                    return@mapIndexed SongStep(beats = beats, tone = name)
                }
                val preset = step?.int("preset")?.takeIf { it in 1..amp.presetCount }
                if (preset == null) {
                    return failure(
                        "step ${index + 1} has preset ${step?.get("preset")}; ${amp.name} has 1..${amp.presetCount}",
                    )
                }
                if (beats == null) {
                    return failure(
                        "step ${index + 1} lasts ${step["beats"]} beats; a step lasts 1..${SongStep.MOST_BEATS}",
                    )
                }
                SongStep(preset, beats)
            }
        return Result.success(Song(amp, tempo, kept, countIn, name))
    }

    private fun JsonObject.int(key: String): Int? = JsonFile.int(this, key)

    private fun failure(why: String): Result<Song> = Result.failure(IllegalArgumentException(why))
}
