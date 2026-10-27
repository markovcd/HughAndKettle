package io.github.markovcd.hughandkettle.amp

import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A loudness match: a soundcheck that brings each of [presets] to the loudness of the [reference] by moving
 * its volume, measuring a held chord each time. The reference is measured first. The caller recalls the
 * preset [at] while the stage is [Stage.Recalling], hands on what the amp plays through [recalled], listens
 * while it is [Stage.Playing] or [Stage.Retry], sends the volume [heard] returns, and calls [adjusted] once
 * the old chord has died.
 *
 * How much a step of the volume knob changes the loudness is not known and differs along the knob, so after
 * the first guess each step is aimed by the slope between the last two measures.
 */
@ConsistentCopyVisibility
public data class LoudnessMatch private constructor(
    val presets: List<Int>,
    val reference: Int,
    val stage: Stage,
    val results: List<MatchResult> = emptyList(),
    /** The reference's level, once measured. */
    val level: Double? = null,
    private val playing: Playing? = null,
    private val points: List<Point> = emptyList(),
) {
    /** Where the match is with the preset [at]. */
    public sealed interface Stage {
        /** The preset is to be recalled, and what the amp plays awaited. */
        public data object Recalling : Stage

        /** A chord is listened for, on the [attempt]th try at this preset. */
        public data class Playing(
            val attempt: Int,
        ) : Stage

        /** The last chord could not be measured, for [why]; another is listened for. */
        public data class Retry(
            val attempt: Int,
            val why: ChordListen.Why,
        ) : Stage

        /** The chord was [off] dB from the reference, so the volume goes [from] one value [to] another. */
        public data class Adjusting(
            val attempt: Int,
            val off: Double,
            val from: Int,
            val to: Int,
        ) : Stage

        /** Every preset is matched, skipped or stopped at. */
        public data object Done : Stage
    }

    /** What the amp played the preset at: its stored volume, its correction then, the volume heard first and now. */
    private data class Playing(
        val stored: Int,
        val correction: Int,
        val start: Int,
        val volume: Int,
    )

    /** One measure: the volume played and how far it was from the reference, in dB. */
    private data class Point(
        val volume: Int,
        val off: Double,
    )

    /** The preset being matched, or null once [Stage.Done]. */
    val at: Int? get() = presets.getOrNull(results.size)?.takeIf { stage != Stage.Done }

    /** Whether the preset [at] is the reference. */
    val measuringReference: Boolean get() = at == reference

    /** The volume the amp plays now, as the match knows it. */
    val volume: Int? get() = (stage as? Stage.Adjusting)?.to ?: playing?.volume

    /** The match once the amp plays the preset [at] with [stored] volume, [correction] and [volume] heard. */
    public fun recalled(
        stored: Int,
        correction: Int,
        volume: Int,
    ): LoudnessMatch =
        if (stage != Stage.Recalling) {
            this
        } else {
            copy(stage = Stage.Playing(1), playing = Playing(stored, correction, volume, volume), points = emptyList())
        }

    /** The match after a listen ended in [outcome], and the volume to send, if any. */
    public fun heard(outcome: ChordListen.Outcome): Pair<LoudnessMatch, Int?> {
        val attempt = attempt ?: return this to null
        val now = playing ?: return this to null
        return when (outcome) {
            is ChordListen.Outcome.Retry -> {
                copy(stage = Stage.Retry(attempt + 1, outcome.why)) to null
            }

            is ChordListen.Outcome.Measured -> {
                if (measuringReference) {
                    copy(
                        level = outcome.level,
                    ).next(MatchResult.Reference(reference, now.stored, now.volume, now.correction)) to
                        null
                } else {
                    measured(attempt, now, Point(now.volume, outcome.level - (level ?: outcome.level)))
                }
            }
        }
    }

    /** The match once the volume [heard] asked for is sent and the old chord has died. */
    public fun adjusted(): LoudnessMatch {
        val adjusting = stage as? Stage.Adjusting ?: return this
        return copy(stage = Stage.Playing(adjusting.attempt + 1), playing = playing?.copy(volume = adjusting.to))
    }

    /** The match with the preset [at] left as it is; skipping the reference stops the match, having nothing to match to. */
    public fun skip(): LoudnessMatch {
        val preset = at ?: return this
        if (preset == reference) return stop()
        return next(MatchResult.Skipped(preset, playing?.stored))
    }

    /** The match ended here, every preset not yet matched skipped. */
    public fun stop(): LoudnessMatch {
        val left = presets.drop(results.size)
        val skipped =
            left.mapIndexed { i, preset ->
                MatchResult.Skipped(
                    preset,
                    if (i ==
                        0
                    ) {
                        playing?.stored
                    } else {
                        null
                    },
                )
            }
        return copy(stage = Stage.Done, results = results + skipped, playing = null, points = emptyList())
    }

    /** The correction each matched preset ends with, by preset; 0 for one that needs none. */
    public fun corrections(): Map<Int, Int> =
        results
            .mapNotNull { result ->
                when (result) {
                    is MatchResult.Reference -> result.preset to result.correction
                    is MatchResult.Matched -> result.preset to result.correction
                    is MatchResult.Skipped -> null
                }
            }.toMap()

    private val attempt: Int?
        get() =
            when (val now = stage) {
                is Stage.Playing -> now.attempt
                is Stage.Retry -> now.attempt
                else -> null
            }

    private fun measured(
        attempt: Int,
        now: Playing,
        point: Point,
    ): Pair<LoudnessMatch, Int?> {
        val points = points + point
        if (abs(point.off) <= CLOSE) return next(matched(now, point, reached = true), points) to null
        val aim = aim(points)
        if (aim == point.volume) return next(matched(now, point, reached = false), points) to null
        if (attempt >= TRIES) {
            val best = points.minBy { abs(it.off) }
            return next(matched(now, best, reached = true), points) to best.volume.takeIf { it != point.volume }
        }
        return copy(stage = Stage.Adjusting(attempt, point.off, point.volume, aim), points = points) to aim
    }

    private fun matched(
        now: Playing,
        point: Point,
        reached: Boolean,
    ): MatchResult.Matched =
        MatchResult.Matched(
            at!!,
            now.stored,
            point.volume,
            (now.correction + point.volume - now.start).coerceIn(-127, 127),
            point.off,
            reached,
        )

    /** The volume aimed at from [points]: by the slope of the last two, or the first guess's; the same at the knob's end. */
    private fun aim(points: List<Point>): Int {
        val last = points.last()
        val before = points.dropLast(1).lastOrNull { it.volume != last.volume }
        val slope =
            before?.let { (last.off - it.off) / (last.volume - it.volume) }?.takeIf { it >= FLATTEST } ?: DB_PER_STEP
        val aimed = (last.volume - last.off / slope).roundToInt()
        // A step too small to round to is still one step, so only the knob's ends stop a match short.
        val moved =
            if (aimed != last.volume) {
                aimed
            } else if (last.off > 0) {
                last.volume - 1
            } else {
                last.volume + 1
            }
        return moved.coerceIn(0, 127)
    }

    private fun next(
        result: MatchResult,
        points: List<Point> = emptyList(),
    ): LoudnessMatch {
        val results = results + result
        val stage = if (results.size >= presets.size) Stage.Done else Stage.Recalling
        return copy(
            stage = stage,
            results = results,
            playing = null,
            points =
                if (stage ==
                    Stage.Done
                ) {
                    points
                } else {
                    emptyList()
                },
        )
    }

    public companion object {
        /** A match of [presets] to [reference], which is among them and measured first. */
        public fun of(
            presets: List<Int>,
            reference: Int,
        ): LoudnessMatch {
            require(reference in presets) { "the reference $reference is not among $presets" }
            return LoudnessMatch(
                listOf(reference) + presets.distinct().filter { it != reference },
                reference,
                Stage.Recalling,
            )
        }

        /** How close to the reference, in dB, counts as matched. */
        public const val CLOSE: Double = 1.0

        // Tries at one preset before the closest is kept.
        private const val TRIES = 5

        // The first guess at how many dB a step of the volume knob moves; the knob's taper is not known.
        private const val DB_PER_STEP = 0.35

        // A slope flatter than this, or going the wrong way, is noise between two measures.
        private const val FLATTEST = 0.05
    }
}
