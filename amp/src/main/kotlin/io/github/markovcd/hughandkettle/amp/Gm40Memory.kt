package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToInt

/**
 * Reads a preset file of H&K's GrandMeister Deluxe 40 Remote app, `.gm40memory`, into tones: an
 * `NSKeyedArchiver` binary property list of the amp's 128 presets. A preset with a name becomes a tone of
 * that name; unnamed ones are the app's empty slots and are left out.
 *
 * The app keeps a knob as 0..100 and the delay time in milliseconds. It has no switch for an effect: an
 * effect whose level is 0 is off, as the amp itself stores one. The power soak is a speaker-off switch and
 * a step from 0 at 1 W.
 */
public object Gm40Memory {
    private const val VERSION = 1L

    private val amp = AmpModels.grandMeisterDeluxe40
    private val delayTime = amp.control("delay_time")!!.scale!!

    // The app's 0..100 field for each knob.
    private val knobs =
        mapOf(
            "gain" to "gain",
            "bass" to "bass",
            "mid" to "mid",
            "treble" to "treble",
            "volume" to "volume",
            "resonance" to "resonance",
            "presence" to "presence",
            "reverbLevel" to "reverb_volume",
            "delayLevel" to "delay_volume",
            "delayFeedback" to "delay_feedback",
            "modIntensity" to "modulation_intensity",
            "modRate" to "modulation_speed",
        )

    // The app's on-off field for each switch it keeps.
    private val switches = mapOf("boost" to "boost", "noiseGate" to "noise_gate", "fxLoop" to "fx_loop")

    // The effect each level switches on.
    private val effects = mapOf("reverbLevel" to "reverb", "delayLevel" to "delay", "modIntensity" to "modulation")

    /** The tones in [bytes]; two presets may share a name. */
    public fun read(bytes: ByteArray): Result<TonesRead> {
        val archive = BinaryPlist.read(bytes).getOrElse { return Result.failure(it) }
        val objects = (archive as? Map<*, *>)?.get("\$objects") as? List<*> ?: return failure("is not a keyed archive")

        fun resolve(value: Any?): Any? = if (value is PlistUid) objects.getOrNull(value.index) else value
        val top = (archive["\$top"] as? Map<*, *>)?.get("root")
        val root = resolve(top) as? Map<*, *> ?: return failure("has no root object")
        val version = root["version"] as? Long
        if (version != VERSION) return failure("is version $version of the app's format; this reads $VERSION")
        val presets =
            (resolve(root["presets"]) as? Map<*, *>)?.get("NS.objects") as? List<*> ?: return failure("has no presets")
        val tones = ArrayList<Tone>()
        val skipped = ArrayList<String>()
        presets.forEachIndexed { index, reference ->
            val preset = resolve(reference) as? Map<*, *>
            val name = (resolve(preset?.get("name")) as? String)?.trim()
            when {
                preset == null -> skipped += "preset ${index + 1} is not a preset"
                name.isNullOrEmpty() || name == "\$null" -> Unit
                else -> tones += Tone(name, amp, values(preset, name, skipped))
            }
        }
        if (tones.isEmpty()) return failure("has no named presets")
        return Result.success(TonesRead(tones, skipped))
    }

    private fun values(
        preset: Map<*, *>,
        name: String,
        skipped: MutableList<String>,
    ): Map<String, Int> {
        val values = LinkedHashMap<String, Int>()

        /** Keeps [value] for [id], or says that [field] held nothing the app would write. */
        fun put(
            id: String,
            field: String,
            value: Int?,
        ) {
            if (value !=
                null
            ) {
                values[id] = value
            } else {
                skipped += "'$name': ${preset[field]} is not a $field the app writes"
            }
        }
        (preset["soundChannel"] as? Long)?.let { put("channel", "soundChannel", selected("channel", it)) }
        for ((field, id) in switches) (preset[field] as? Boolean)?.let { values[id] = if (it) 127 else 0 }
        for ((field, id) in knobs) {
            val percent = preset[field] as? Double ?: continue
            put(id, field, if (percent in 0.0..100.0) (percent * 127 / 100).roundToInt() else null)
        }
        for ((field, id) in effects) (preset[field] as? Double)?.let { values[id] = if (it > 0) 127 else 0 }
        (preset["delayTime"] as? Double)?.let { ms ->
            put("delay_time", "delayTime", if (ms.isFinite()) delayTime.value(ms.roundToInt()) else null)
        }
        (preset["modType"] as? Long)?.let { put("modulation_type", "modType", selected("modulation_type", it)) }
        val power = preset["speakerPower"] as? Long
        when (preset["speakerOff"] as? Boolean) {
            true -> put("power_soak", "speakerOff", selected("power_soak", 0))
            false -> if (power != null) put("power_soak", "speakerPower", selected("power_soak", power + 1))
            null -> Unit
        }
        return values
    }

    /** The value that selects option [index] of the selector [id], or null when it has no such option. */
    private fun selected(
        id: String,
        index: Long,
    ): Int? {
        val options = (amp.control(id)!!.kind as ControlKind.Selector).options.size
        return if (index in 0 until options) Sectors.value(index.toInt(), options) else null
    }

    private fun failure(why: String): Result<TonesRead> =
        Result.failure(IllegalArgumentException("the preset file $why"))
}
