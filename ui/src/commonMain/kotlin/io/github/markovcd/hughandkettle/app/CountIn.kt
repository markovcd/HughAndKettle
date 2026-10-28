package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.Song
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/**
 * The bar of [click]s before a song plays or a take starts, each played the sound output's [lag] ahead of its
 * beat so it is heard on the beat the amp switches on. Playing a song and recording a take share it.
 */
internal class CountIn(
    private val click: () -> Unit,
    private val lag: suspend () -> Duration,
) {
    /** The clicks still to come, this one among them, or null while not counting in. */
    var left by mutableStateOf<Int?>(null)
        private set

    /**
     * Where the first beat of [song] falls when counted in from now, measured so the count-in runs in negative
     * time, and how far ahead of its beat a click is played: the sound output's lag when there are [clicks].
     */
    suspend fun firstBeat(
        song: Song,
        clicks: Boolean,
    ): Pair<TimeMark, Duration> {
        val late = if (clicks) lag().coerceIn(Duration.ZERO, MOST_LAG) else Duration.ZERO
        return TimeSource.Monotonic.markNow() + song.lead + late to late
    }

    /** Clicks the count-in of [song], unless it is off, each click [late] ahead of its beat before the first at [started]. */
    suspend fun run(
        song: Song,
        started: TimeMark,
        late: Duration,
    ) {
        val counts = if (song.countIn) Song.COUNT_IN_BEATS else 0
        repeat(counts) {
            left = counts - it
            click()
            delay(song.beat * (it + 1) - song.lead - late - started.elapsedNow())
        }
    }

    /** Ends the count-in, on the first beat or when stopped. */
    fun over() {
        left = null
    }

    private companion object {
        /** The most a click is played ahead of its beat; a sound output slower than this is mismeasured. */
        val MOST_LAG = 500.milliseconds
    }
}
