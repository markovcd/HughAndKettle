package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.TimeSource

/**
 * The song of the panel's amp: its tempo and steps, edited and kept on the phone, and played from a tap. While
 * it plays, each step recalls its preset on its beat with the delay time at one repeat a beat, and the tempo
 * lock holds that time across the recalls. A song that cannot be read plays nothing, says why in [failure],
 * and is left as it is.
 */
class SongState(
    private val store: SongStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private var all by mutableStateOf(emptyMap<AmpModel, Song>())
    private var unreadable by mutableStateOf(emptySet<AmpModel>())
    private var playing: Job? = null

    /** Whether the song turned the tempo lock on, so it turns it off again when it ends. */
    private var locking = false

    var failure by mutableStateOf<String?>(null)
        private set

    /** Where the playing song stands, or null while none plays. */
    var position by mutableStateOf<Song.Position?>(null)
        private set

    init {
        AmpModels.all.forEach { amp ->
            scope.launch {
                val read = withContext(io) { store.song(amp) }
                read.onFailure {
                    failure = it.message
                    unreadable = unreadable + amp
                }
                all = all + (amp to read.getOrElse { Song(amp) })
            }
        }
    }

    /** The song of the panel's amp. */
    val song: Song get() = all[panel.amp] ?: Song(panel.amp)

    /** Whether the song can be changed: not while it plays, nor when its file could not be read. */
    val editable: Boolean get() = position == null && panel.amp !in unreadable

    fun tempo(bpm: Int) = change { it.copy(tempo = bpm.coerceIn(Song.TEMPOS)) }

    /** Adds a step on the preset the amp is on, else the first. */
    fun add() = change { it.copy(steps = it.steps + SongStep(panel.state.preset ?: 1, NEW_STEP_BEATS)) }

    fun beats(
        step: Int,
        beats: Int,
    ) = change { song ->
        song.copy(
            steps =
                song.steps.mapIndexed { i, it ->
                    if (i ==
                        step
                    ) {
                        it.copy(beats = beats.coerceIn(1, SongStep.MOST_BEATS))
                    } else {
                        it
                    }
                },
        )
    }

    fun remove(step: Int) = change { song -> song.copy(steps = song.steps.filterIndexed { i, _ -> i != step }) }

    /** Plays the song from its first beat, now. */
    fun start() {
        val song = song
        if (playing != null || song.steps.isEmpty() || !panel.linked) return
        val time = song.amp.controls.firstOrNull(Control::timed)
        playing =
            scope.launch {
                val started = TimeSource.Monotonic.markNow()
                var cued = -1
                while (true) {
                    val elapsed = started.elapsedNow()
                    val at = song.at(elapsed) ?: break
                    if (at.step != cued) {
                        if (cued < 0 && time != null && panel.lock == null) {
                            panel.lock(time, on = true)
                            locking = true
                        }
                        panel.cue(song.cue(at.step))
                        cued = at.step
                    }
                    position = at
                    delay(song.beat * ((elapsed / song.beat).toInt() + 1) - started.elapsedNow())
                }
                ended()
            }
    }

    fun stop() {
        val job = playing ?: return
        job.cancel()
        ended()
    }

    private fun ended() {
        val held = panel.lock
        if (locking && held != null) panel.lock(held.control, on = false)
        locking = false
        position = null
        playing = null
    }

    private fun change(how: (Song) -> Song) {
        if (!editable) return
        val after = how(song)
        all = all + (after.amp to after)
        scope.launch { withContext(io) { store.save(after) }.onFailure { failure = it.message } }
    }

    private companion object {
        // Four bars of four: a short part, lengthened from there.
        const val NEW_STEP_BEATS = 16
    }
}
