package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * A run of the amp's presets at one [tempo], in beats per minute: each [step] recalls its preset and holds
 * it for its beats, and the delay time is set to one repeat a beat. The clock is the caller's; a song only
 * says what plays when.
 */
public data class Song(
    val amp: AmpModel,
    val tempo: Int = DEFAULT_TEMPO,
    val steps: List<SongStep> = emptyList(),
) {
    init {
        require(tempo in TEMPOS) { "a tempo is ${TEMPOS.first}..${TEMPOS.last} BPM, got $tempo" }
        val outside = steps.map(SongStep::preset).filter { it !in 1..amp.presetCount }
        require(outside.isEmpty()) { "${amp.name} has presets 1..${amp.presetCount}, not $outside" }
    }

    /** Where a song stands: the [step] counted from 0, and the [beat] of it counted from 1. */
    public data class Position(
        val step: Int,
        val beat: Int,
    )

    /** How long one beat lasts. */
    val beat: Duration get() = 1.minutes / tempo

    /** How long the whole song lasts. */
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

    /** The messages that start [step]: its preset, then the delay time, so the preset's own gives way to the song's. */
    public fun cue(step: Int): List<MidiMessage> = listOfNotNull(amp.recall(steps[step].preset), delayTime)

    public companion object {
        /** The tempos a song takes, in beats per minute. */
        public val TEMPOS: IntRange = 20..400

        public const val DEFAULT_TEMPO: Int = 120
    }
}
