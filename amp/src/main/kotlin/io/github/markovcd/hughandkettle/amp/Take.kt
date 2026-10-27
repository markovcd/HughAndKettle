package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

/**
 * A song being recorded at [tempo]: each sound the amp was switched to, and how long after the first beat. A
 * switch lands on the nearest beat, so each step lasts whole beats, and of switches landing on the same beat
 * only the last is kept. A switch the amp reported is moved back by how late its report comes, and a report of
 * the preset just switched to is that same switch, not a second one. The clock is the caller's.
 */
public data class Take(
    val tempo: Int,
    val marks: List<Mark> = emptyList(),
) {
    /** The [sound] switched to [at] after the first beat, its beats not known until the next switch. */
    public data class Mark(
        val at: Duration,
        val sound: SongStep,
    )

    /** How long one beat lasts. */
    val beat: Duration get() = 1.minutes / tempo

    /** The take after [switch], [at] after the first beat, playing the delay at [note] if given. */
    public fun after(
        switch: SoundSwitch,
        at: Duration,
        note: NoteLength? = null,
    ): Take {
        val time = if (switch.reported) at - REPORT_LAG else at
        val last = marks.lastOrNull()
        if (switch.reported && last != null && last.sound.preset == switch.preset && time - last.at < SAME_SWITCH) {
            return this
        }
        return copy(marks = marks + Mark(time, SongStep(switch.preset, 1, switch.tone, note)))
    }

    /** The steps taken, the last one held until [end] after the first beat, and for a beat at least. */
    public fun steps(end: Duration): List<SongStep> {
        var floor = 0L
        val starts =
            marks.map { mark ->
                floor = maxOf(floor, beats(mark.at))
                floor to mark.sound
            }
        val kept = starts.filterIndexed { index, (start, _) -> starts.getOrNull(index + 1)?.first != start }
        return kept.flatMapIndexed { index, (start, sound) ->
            val next = kept.getOrNull(index + 1)?.first ?: maxOf(beats(end), start + 1)
            held(sound, next - start)
        }
    }

    /** The beat [at] lands on, counted from 0, never before the first. */
    private fun beats(at: Duration): Long = (at / beat).roundToLong().coerceAtLeast(0)

    /** [sound] held for [beats], as one step or, past the most a step lasts, as several in a row. */
    private fun held(
        sound: SongStep,
        beats: Long,
    ): List<SongStep> {
        val whole = List((beats / SongStep.MOST_BEATS).toInt()) { sound.copy(beats = SongStep.MOST_BEATS) }
        val rest = (beats % SongStep.MOST_BEATS).toInt()
        return if (rest > 0) whole + sound.copy(beats = rest) else whole
    }

    public companion object {
        /** How long after a footswitch recall the amp reports it, timed on a GrandMeister Deluxe 40 (docs/wmi-1.md). */
        public val REPORT_LAG: Duration = 120.milliseconds

        // A report this close to a switch to the same preset answers that switch; nobody recalls a preset twice faster.
        private val SAME_SWITCH = 300.milliseconds

        /** The first of "Take 1", "Take 2" and on that is not [taken]. */
        public fun name(taken: (String) -> Boolean): String =
            generateSequence(1) { it + 1 }.map { "Take $it" }.first { !taken(it) }
    }
}
