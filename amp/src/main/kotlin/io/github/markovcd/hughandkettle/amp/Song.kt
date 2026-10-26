package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A run of the amp's presets and tones at one [tempo], in beats per minute: each [step] recalls its preset or
 * tone and holds it for its beats, and while timed the delay time is set to one repeat a beat. With [countIn], a bar of clicks comes
 * first. A song kept on the phone has a [name], unique for its amp; a blank one is untitled. The clock is
 * the caller's; a song only says what plays when.
 */
public data class Song(
    val amp: AmpModel,
    val tempo: Int = DEFAULT_TEMPO,
    val steps: List<SongStep> = emptyList(),
    val countIn: Boolean = true,
    val name: String = "",
) {
    init {
        require(tempo in TEMPOS) { "a tempo is ${TEMPOS.first}..${TEMPOS.last} BPM, got $tempo" }
        val outside = steps.mapNotNull(SongStep::preset).filter { it !in 1..amp.presetCount }
        require(outside.isEmpty()) { "${amp.name} has presets 1..${amp.presetCount}, not $outside" }
    }

    /** Where a song stands: the [step] counted from 0, and the [beat] of it counted from 1. */
    public data class Position(
        val step: Int,
        val beat: Int,
    )

    /** How long one beat lasts. */
    val beat: Duration get() = 1.minutes / tempo

    /** How long the count-in lasts before the first step: a bar, or nothing without one. */
    val lead: Duration get() = if (countIn) beat * COUNT_IN_BEATS else Duration.ZERO

    /** How long the whole song lasts, from its first step. */
    val length: Duration get() = beat * steps.sumOf(SongStep::beats)

    /** How far into the song [step] starts. */
    public fun startOf(step: Int): Duration = beat * steps.take(step).sumOf(SongStep::beats)

    /** Where the song stands [elapsed] after it started, or null before it or once it is over. */
    public fun at(elapsed: Duration): Position? {
        if (elapsed.isNegative()) return null
        var beats = (elapsed / beat).toLong()
        steps.forEachIndexed { index, step ->
            if (beats < step.beats) return Position(index, beats.toInt() + 1)
            beats -= step.beats
        }
        return null
    }

    /** The delay time at one repeat a beat, as near as the amp's time knob reaches; null on an amp with none. */
    val delayTime: ControlChange?
        get() {
            val time = amp.controls.firstOrNull(Control::timed) ?: return null
            val scale = time.scale ?: return null
            return time.set(scale.value((beat.inWholeMilliseconds).toInt()))
        }

    /** The delay time [delayTime] sets, in milliseconds as the knob reaches them; null on an amp with no time knob. */
    val delayMs: Int?
        get() {
            val time = delayTime ?: return null
            return amp.controls
                .firstOrNull(Control::timed)
                ?.scale
                ?.amount(time.value)
        }

    /**
     * The messages that start [step]: its preset, or the values of its tone found by [tone]. When [timed], the
     * delay time follows, so the preset's own gives way to the song's and the tone's is never sent; otherwise
     * each keeps its own. A tone missing, or kept for another amp, cues the delay time alone, or nothing, and
     * the step before keeps sounding.
     */
    public fun cue(
        step: Int,
        timed: Boolean,
        tone: (String) -> Tone?,
    ): List<MidiMessage> {
        val at = steps[step]
        val time = delayTime?.takeIf { timed }
        val sound =
            at.preset?.let { listOf(amp.recall(it)) }
                ?: at.tone
                    ?.let(tone)
                    ?.takeIf { it.amp == amp }
                    ?.recall()
                    ?.filterNot { time != null && it.controller == time.controller }
                    .orEmpty()
        return sound + listOfNotNull(time)
    }

    public companion object {
        /** The tempos a song takes, in beats per minute. */
        public val TEMPOS: IntRange = 20..400

        public const val DEFAULT_TEMPO: Int = 120

        /** The clicks of a count-in: one bar of four. */
        public const val COUNT_IN_BEATS: Int = 4
    }
}
