package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.CappedRead
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongFile
import io.github.markovcd.hughandkettle.amp.SongImport
import io.github.markovcd.hughandkettle.amp.SongList
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.TapTempo
import io.github.markovcd.hughandkettle.amp.Tone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * The songs of the panel's amp, kept on the phone in the person's order, and the one open: its tempo and steps,
 * edited and kept as they change, and played from a tap. A bar of [click]s counts it in, unless its count-in is
 * off, each played the sound output's [lag] ahead of its beat so it is heard on the beat the amp switches on.
 * [takes] records a take of it.
 * The open song's tempo is the one tempo: while the tempo lock is on, the delay time is a [note] at it, moved
 * whenever either changes, and a time the amp sets itself (its knob, a footswitch tap) sets the tempo instead.
 * While it plays, each step recalls its preset, or sends its tone from [tones], on its beat; while locked, the
 * delay time follows and the lock holds it across the recalls, and otherwise each step keeps its own. No other
 * song can be opened while one plays, nor during a take. An amp with no songs opens on an untitled one, kept
 * once it has a step or a name. A song that cannot be read plays nothing, says why in
 * [failure], and is left as it is. The folder is read and written off the main thread, one change at a time.
 */
class SongState(
    private val store: SongStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    click: () -> Unit = {},
    lag: suspend () -> Duration = { Duration.ZERO },
    private val tones: () -> List<Tone> = { emptyList() },
    recordClick: Flow<Boolean> = flowOf(false),
    keepRecordClick: suspend (Boolean) -> Unit = {},
) {
    private var lists by mutableStateOf(emptyMap<AmpModel, SongList>())
    private var opened by mutableStateOf(emptyMap<AmpModel, String>())

    /** The untitled song of an amp whose open song is not kept yet. */
    private var drafts by mutableStateOf(emptyMap<AmpModel, Song>())
    private var notices by mutableStateOf(emptyMap<AmpModel, SongNotice>())
    private var playing by mutableStateOf<Job?>(null)
    private var position by mutableStateOf<Song.Position?>(null)
    private val counting = CountIn(click, lag)
    private var taps = TapTempo()
    private val writes = Mutex()

    private var unsaved by mutableStateOf<String?>(null)

    /** Why the open song cannot be read, or why a change could not be saved. */
    val failure: String? get() = openKept()?.problem ?: unsaved

    /** The take of the open song, recorded while the person plays. */
    val takes: TakeState = TakeState(this, panel, scope, counting, click, recordClick, keepRecordClick)

    /** Whether the delay time is a note at the song's tempo: only while the tempo lock is on. */
    val timed: Boolean get() = panel.lock != null

    /** The note the delay repeats at while the tempo lock is on. */
    var note by mutableStateOf(NoteLength.QUARTER)
        private set

    /** The note the delay repeats at now: the playing step's own, else the one picked. */
    val sounding: NoteLength get() = position?.let { song.steps.getOrNull(it.step)?.note } ?: note

    /** Whether the tempo was last set by the amp's own delay time, rather than on the phone. */
    var tempoFromAmp by mutableStateOf(false)
        private set

    /** What the open song is doing. */
    val now: Now
        get() {
            val left = counting.left
            return when {
                left != null && takes.recording -> {
                    Now.CountingIn(
                        left,
                        takes.take
                            ?.marks
                            ?.firstOrNull()
                            ?.sound,
                        take = true,
                    )
                }

                left != null -> {
                    Now.CountingIn(left, song.steps.firstOrNull(), take = false)
                }

                takes.recording -> {
                    Now.Recording
                }

                else -> {
                    position?.let(Now::Playing) ?: Now.Stopped
                }
            }
        }

    init {
        AmpModels.all.forEach { amp -> scope.launch { writes.withLock { refresh(amp) } } }
        scope.launch { panel.held.collect(::followAmp) }
    }

    /** The open song of the panel's amp. */
    val song: Song
        get() {
            val kept = openKept()
            return kept?.song ?: kept?.let { Song(panel.amp, name = it.name) } ?: drafts[panel.amp] ?: Song(panel.amp)
        }

    /** The songs kept for the panel's amp, in their order. */
    val songs: List<KeptSong> get() = lists[panel.amp]?.songs.orEmpty()

    /** Whether the open song is kept, rather than an untitled one not kept yet. */
    val kept: Boolean get() = openKept() != null

    /** What the song list has to tell: a song carried over, an import set apart or refused, a change refused. */
    val notice: SongNotice? get() = notices[panel.amp]

    /** Whether the song can be changed: not while it counts in or plays, nor during a take, nor when its file could not be read. */
    val editable: Boolean get() = choosable && openKept().let { it == null || it.song != null }

    /** Whether another song can be opened: not while one counts in or plays, nor during a take. */
    val choosable: Boolean get() = playing == null && !takes.recording

    /** Whether the song is counting in or playing. */
    val busy: Boolean get() = playing != null

    /** Whether a song of the panel's amp is already called [name], regardless of case. */
    fun taken(name: String): Boolean = lists[panel.amp]?.find(name.trim()) != null

    fun open(name: String) {
        val amp = panel.amp
        val kept = lists[amp]?.find(name)
        if (!choosable || kept?.song == null) return
        opened = opened + (amp to kept.name)
        drafts = drafts - amp
        takes.forget()
        taps = TapTempo()
        queue { store.open(amp, kept.name) }
        retime()
    }

    /** Makes and opens a song called [name], at the starting tempo with the count-in on and no steps. */
    fun create(name: String) {
        val amp = panel.amp
        val trimmed = name.trim()
        if (!choosable || trimmed.isEmpty() || taken(trimmed)) return
        takes.forget()
        keepOpened(Song(amp, name = trimmed))
        retime()
    }

    /** Renames the song [name]; the untitled song, not kept yet, is kept under its new name. */
    fun rename(
        name: String,
        to: String,
    ) {
        val trimmed = to.trim()
        if (trimmed.isEmpty() || !choosable) return
        if (!kept && name == song.name) return change { it.copy(name = trimmed) }
        val amp = panel.amp
        val open = name.equals(song.name, ignoreCase = true)
        listed(amp) { store.rename(amp, name, trimmed).mapCatching { if (open) store.open(amp, trimmed).getOrThrow() } }
    }

    /** Keeps a copy of the song [name] right after it, named apart from the rest. */
    fun duplicate(name: String) {
        val amp = panel.amp
        listed(amp) { store.duplicate(amp, name) }
    }

    fun delete(name: String) {
        val amp = panel.amp
        if (!choosable) return
        listed(amp) { store.delete(amp, name) }
    }

    /** Moves the song [name] [by] places, up for negative; past either end it stays where it is. */
    fun move(
        name: String,
        by: Int,
    ) {
        val amp = panel.amp
        val list = lists[amp] ?: return
        val position = list.songs.indexOfFirst { it.name.equals(name, ignoreCase = true) } + 1 + by
        if (position in 1..list.songs.size) listed(amp) { store.move(amp, name, position) }
    }

    /** Writes the song [name] in the song file format to what [open] opens. */
    fun export(
        name: String,
        open: () -> OutputStream?,
    ) {
        val amp = panel.amp
        val song = lists[amp]?.find(name)?.song ?: return
        listed(amp) {
            try {
                val stream = open() ?: return@listed Result.failure(IOException("nowhere to write"))
                stream.use { it.write(SongFile.write(song).toByteArray()) }
                Result.success(Unit)
            } catch (e: IOException) {
                Result.failure(e)
            }
        }
    }

    /**
     * Reads a song file from what [open] opens and keeps its song at the end of the list, named apart from
     * the rest. A song for another amp model is refused whole.
     */
    fun import(open: () -> InputStream?) {
        val amp = panel.amp
        listed(amp) {
            val text =
                try {
                    val stream = open() ?: return@listed Result.failure(IOException("nothing to read"))
                    stream.use { CappedRead.bytes(it, SongFile.MOST_BYTES) }?.decodeToString()
                } catch (e: IOException) {
                    return@listed Result.failure(e)
                } ?: return@listed Result.failure(IOException("larger than a song file can be"))
            store.import(amp, text).map {
                when (it) {
                    is SongImport.OtherAmp -> {
                        SongNotice.OtherAmp(AmpModels.byId(it.amp)?.name ?: it.amp)
                    }

                    is SongImport.Kept -> {
                        val renamed = it.name != it.asked && it.asked.isNotBlank()
                        if (renamed) SongNotice.ImportedAs(it.asked, it.name) else null
                    }
                }
            }
        }
    }

    fun dismiss() {
        notices = notices - panel.amp
    }

    fun tempo(bpm: Int) {
        change { it.copy(tempo = bpm.coerceIn(Song.TEMPOS)) }
        tempoFromAmp = false
        retime()
    }

    /** Turns the tempo lock on, holding a [note] at the song's tempo, or off, leaving each preset its own time. */
    fun lock(on: Boolean) {
        val time = panel.amp.controls.firstOrNull(Control::timed) ?: return
        panel.lock(time, on)
        retime()
    }

    /** Sets the note the delay repeats at, moving the delay time to it while locked. */
    fun note(note: NoteLength) {
        this.note = note
        retime()
    }

    /** Gives [step] a note of its own while the tempo lock is on, or with null leaves it on the note picked. */
    fun note(
        step: Int,
        note: NoteLength?,
    ) = changeStep(step) { it.copy(note = note) }

    fun countIn(on: Boolean) = change { it.copy(countIn = on) }

    /** A tap on the song's beat at [at], from any fixed point; from the second tap on, the tempo follows the taps. */
    fun tap(at: Duration) {
        taps = taps.after(at)
        val beat = taps.beat ?: return
        tempo((1.minutes / beat).roundToInt())
    }

    /** Adds a step on the preset the amp is on, else the first. */
    fun add() = change { it.copy(steps = it.steps + SongStep(panel.state.preset ?: 1, NEW_STEP_BEATS)) }

    /** Adds a step on [tone]. */
    fun add(tone: Tone) = change { it.copy(steps = it.steps + SongStep(beats = NEW_STEP_BEATS, tone = tone.name)) }

    /** The tone [name] for the panel's amp, regardless of case; null once it is deleted. */
    fun tone(name: String): Tone? =
        tones().firstOrNull { it.amp == panel.amp && it.name.equals(name, ignoreCase = true) }

    /** Whether [step] is on a tone that is no longer kept for the panel's amp. */
    fun missing(step: SongStep): Boolean = step.tone?.let(::tone) == null && step.tone != null

    /** Renames [tone] to [to] in the steps of every song of its amp, the untitled one not kept yet among them. */
    fun renameTone(
        tone: Tone,
        to: String,
    ) {
        val amp = tone.amp
        drafts[amp]?.let { draft ->
            drafts =
                drafts +
                (
                    amp to
                        draft.copy(
                            steps = draft.steps.map { if (it.isTone(tone.name)) it.copy(tone = to) else it },
                        )
                )
        }
        listed(amp) { store.renameTone(amp, tone.name, to) }
    }

    fun beats(
        step: Int,
        beats: Int,
    ) = changeStep(step) { it.copy(beats = beats.coerceIn(1, SongStep.MOST_BEATS)) }

    /** Moves [step] to stand at [to], the steps between shifting over to make room. */
    fun move(
        step: Int,
        to: Int,
    ) = change { song ->
        if (step !in song.steps.indices || to !in song.steps.indices) return@change song
        song.copy(steps = song.steps.toMutableList().apply { add(to, removeAt(step)) })
    }

    fun remove(step: Int) = change { song -> song.copy(steps = song.steps.filterIndexed { i, _ -> i != step }) }

    /** Plays the song: its count-in from now, then its first step on the beat after. */
    fun start() {
        val song = song
        if (!choosable || song.steps.isEmpty() || !panel.linked) return
        takes.forget()
        playing =
            scope.launch {
                val (started, late) = counting.firstBeat(song, clicks = song.countIn)
                counting.run(song, started, late)
                delay(-started.elapsedNow())
                counting.over()
                var cued = -1
                while (true) {
                    val elapsed = started.elapsedNow().coerceAtLeast(Duration.ZERO)
                    val at = song.at(elapsed) ?: break
                    if (at.step != cued) {
                        panel.cue(song.cue(at.step, note.takeIf { timed }, ::tone))
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
        counting.over()
        position = null
        playing = null
    }

    /** Keeps [song] at the end of its amp's list, opened. */
    internal fun keepOpened(song: Song) {
        keep(song, replacing = null)
        drafts = drafts - song.amp
        queue { store.save(song).mapCatching { store.open(song.amp, song.name).getOrThrow() } }
    }

    /** Deletes the song [name] and opens [reopen], else none. */
    internal fun drop(
        name: String,
        reopen: String?,
    ) {
        val amp = panel.amp
        opened = if (reopen != null) opened + (amp to reopen) else opened - amp
        listed(amp) { store.delete(amp, name).mapCatching { if (reopen != null) store.open(amp, reopen).getOrThrow() } }
    }

    /** The name of the open song while it is kept. */
    internal val openName: String? get() = openKept()?.name

    /** Moves the delay time to the note at the song's tempo, while the tempo lock is on. */
    private fun retime() {
        if (!timed) return
        val time = song.delayTime(sounding) ?: return
        val control = panel.amp.controls.firstOrNull(Control::timed) ?: return
        panel.set(control, time.value)
    }

    /** Takes a delay time the amp set itself as the tempo at which the note lasts that long. */
    private fun followAmp(value: Int) {
        val scale =
            panel.amp.controls
                .firstOrNull(Control::timed)
                ?.scale ?: return
        if (!editable || value == song.delayTime(note)?.value) return
        val bpm = note.tempo(scale.amount(value)).coerceIn(Song.TEMPOS)
        if (bpm == song.tempo) return
        change { it.copy(tempo = bpm) }
        tempoFromAmp = true
    }

    private fun changeStep(
        step: Int,
        how: (SongStep) -> SongStep,
    ) = change { song -> song.copy(steps = song.steps.mapIndexed { i, it -> if (i == step) how(it) else it }) }

    private fun change(how: (Song) -> Song) {
        if (!editable) return
        takes.forget()
        val amp = panel.amp
        val after = how(song)
        if (!kept && after.steps.isEmpty() && after.name.isBlank()) {
            drafts = drafts + (amp to after)
            return
        }
        val first = !kept
        keep(after, replacing = if (first) null else song.name)
        drafts = drafts - amp
        queue {
            store.save(after, replace = true).mapCatching { if (first) store.open(amp, after.name).getOrThrow() }
        }
    }

    /** Shows [song] as the open song of its amp, in place of the kept song named [replacing], else at the end. */
    private fun keep(
        song: Song,
        replacing: String?,
    ) {
        val list = lists[song.amp] ?: SongList(emptyList())
        val at = replacing?.let { name -> list.songs.indexOfFirst { it.name.equals(name, ignoreCase = true) } } ?: -1
        val entry = KeptSong(song.name, song, null)
        val songs = if (at < 0) list.songs + entry else list.songs.toMutableList().apply { this[at] = entry }
        lists = lists + (song.amp to list.copy(songs = songs))
        opened = opened + (song.amp to song.name)
    }

    private fun openKept(): KeptSong? = opened[panel.amp]?.let { lists[panel.amp]?.find(it) }

    /** Runs [action] off the main thread after the changes before it, and says why when it fails. */
    private fun queue(action: () -> Result<Any?>) {
        scope.launch {
            writes.withLock {
                withContext(io) { action() }
                    .onSuccess { unsaved = null }
                    .onFailure { unsaved = it.message }
            }
        }
    }

    /** Runs [action] as [queue] does, then reads the list of [amp] afresh and tells what came of it. */
    private fun listed(
        amp: AmpModel,
        action: () -> Result<Any?>,
    ) {
        scope.launch {
            writes.withLock {
                val result = withContext(io) { action() }
                result
                    .onSuccess {
                        notices =
                            (it as? SongNotice)?.let { notice -> notices + (amp to notice) } ?: (notices - amp)
                    }.onFailure {
                        notices = notices + (
                            amp to
                                SongNotice.Failed(
                                    it.message ?: "${it::class.simpleName}",
                                )
                        )
                    }
                refresh(amp)
            }
        }
    }

    /** Reads the songs of [amp] and opens the one marked open, else the first that can be read. */
    private suspend fun refresh(amp: AmpModel) {
        val read = withContext(io) { store.list(amp) }
        val list =
            read.getOrElse {
                unsaved = it.message
                return
            }
        lists = lists + (amp to list)
        val open =
            list.open ?: opened[amp]?.takeIf { list.find(it) != null }
                ?: list.songs.firstOrNull { it.song != null }?.name
        opened = if (open != null) opened + (amp to open) else opened - amp
        list.carried?.let { notices = notices + (amp to SongNotice.Carried(it)) }
    }

    companion object {
        /** The beats a step is added with: four bars of four, a short part, lengthened from there. */
        const val NEW_STEP_BEATS = 16
    }

    /** What the open song is doing. */
    sealed interface Now {
        data object Stopped : Now

        /** Counting in before [first]: [left] clicks to come, this one among them; the count-in of a take if [take]. */
        data class CountingIn(
            val left: Int,
            val first: SongStep?,
            val take: Boolean,
        ) : Now

        data class Playing(
            val position: Song.Position,
        ) : Now

        /** Recording a take, from its first beat on; [takes] holds it. */
        data object Recording : Now
    }
}
