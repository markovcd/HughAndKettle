package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SoundSwitch
import io.github.markovcd.hughandkettle.amp.Take
import io.github.markovcd.hughandkettle.amp.Tone
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark

/**
 * A take of the open song: the sounds switched to while the person plays, at the song's tempo, counted in as
 * the song is and clicking every beat if [recordClick] says so. At Stop it is kept as a song of its own,
 * opened and offered for discard until the song is changed or another is opened.
 */
class TakeState internal constructor(
    private val songs: SongState,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val counting: CountIn,
    private val click: () -> Unit,
    recordClick: Flow<Boolean>,
    private val keepRecordClick: suspend (Boolean) -> Unit,
) {
    private var taking by mutableStateOf<Job?>(null)

    /** Where the first beat of the take is, so a switch is timed from it; the count-in runs before it. */
    private var started: TimeMark? = null

    /** The song open before the take, opened again if the take is discarded. */
    private var before: String? = null

    /** The take being recorded, or null while none is. */
    var take by mutableStateOf<Take?>(null)
        private set

    /** The name the take will be kept under. */
    var name by mutableStateOf("")
        private set

    /** How far the take is past its first beat, from the last beat on; null before it. */
    var at by mutableStateOf<Duration?>(null)
        private set

    /** The take just kept, offered for discard until the song is changed or another is opened. */
    var kept by mutableStateOf<String?>(null)
        private set

    /** Whether every beat of a take clicks, for playing without a drummer. */
    var clicking by mutableStateOf(false)
        private set

    /** Whether a take is being recorded, its count-in among it. */
    val recording: Boolean get() = taking != null

    init {
        scope.launch { panel.switches.collect(::switched) }
        scope.launch { recordClick.collect { clicking = it } }
    }

    /**
     * Records a take at the open song's tempo, counted in as the song is. It starts on [from], else on the preset
     * the amp is on; each switch of sound after that starts a step, until Stop or the link is lost.
     */
    fun record(from: Tone? = null) {
        val song = songs.song
        if (!songs.choosable || !panel.linked) return
        val note = songs.note.takeIf { songs.timed }
        val first = from?.let { SoundSwitch(tone = it.name) } ?: panel.state.preset?.let { SoundSwitch(it) }
        before = songs.openName
        kept = null
        name = Take.name(songs::taken)
        take = Take(song.tempo).let { take -> first?.let { take.after(it, Duration.ZERO, note) } ?: take }
        taking =
            scope.launch {
                val (beat, late) = counting.firstBeat(song, clicks = song.countIn || clicking)
                started = beat
                counting.run(song, beat, late)
                var beats = 0
                while (panel.linked) {
                    val on = song.beat * beats
                    if (clicking) {
                        delay(on - late - beat.elapsedNow())
                        click()
                    }
                    delay(on - beat.elapsedNow())
                    counting.over()
                    at = on
                    beats++
                }
                keep()
            }
    }

    /** Ends the take and keeps it as a song of its own, opened; a take stopped before its first beat is not kept. */
    fun stop() {
        val job = taking ?: return
        job.cancel()
        keep()
    }

    /** Deletes the take just kept and opens again the song open before it. */
    fun discard() {
        val name = kept ?: return
        kept = null
        songs.drop(name, reopen = before)
    }

    /** Clicks every beat of a take from now on, or not; remembered through [keepRecordClick]. */
    fun recordClick(on: Boolean) {
        clicking = on
        scope.launch { keepRecordClick(on) }
    }

    /** Stops offering the take just kept for discard, for a song changed or another opened. */
    internal fun forget() {
        kept = null
    }

    private fun keep() {
        val take = take ?: return
        val end = started?.elapsedNow() ?: Duration.ZERO
        val countIn = songs.song.countIn
        this.take = null
        taking = null
        started = null
        at = null
        counting.over()
        val steps = if (end.isNegative()) emptyList() else take.steps(end)
        if (steps.isEmpty()) return
        val song = Song(panel.amp, take.tempo, steps, countIn, name)
        songs.keepOpened(song)
        kept = song.name
    }

    /** Adds [switch] to the take, timed from its first beat, once the take has one. */
    private fun switched(switch: SoundSwitch) {
        val take = take ?: return
        val started = started ?: return
        this.take = take.after(switch, started.elapsedNow(), songs.note.takeIf { songs.timed })
    }
}
