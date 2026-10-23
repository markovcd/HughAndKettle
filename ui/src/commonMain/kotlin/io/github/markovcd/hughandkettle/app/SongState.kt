package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongFile
import io.github.markovcd.hughandkettle.amp.SongList
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.TapTempo
import io.github.markovcd.hughandkettle.amp.Tempo
import io.github.markovcd.hughandkettle.amp.Tone
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

/**
 * The songs of the panel's amp, kept on the phone in the person's order, and the one open: its tempo and steps,
 * edited and kept as they change, and played from a tap. A bar of [click]s counts it in, unless its count-in is
 * off, each played the sound output's [lag] ahead of its beat so it is heard on the beat the amp switches on.
 * While it plays, each step recalls its preset, or sends its tone from [tones], on its beat with the delay time
 * at one repeat a beat, and the tempo lock holds that time across the recalls; no other song can be opened. An amp with no songs opens on an
 * untitled one, kept once it has a step or a name. A song that cannot be read plays nothing, says why in
 * [failure], and is left as it is. The folder is read and written off the main thread, one change at a time.
 */
class SongState(
    private val store: SongStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val click: () -> Unit = {},
    private val lag: suspend () -> Duration = { Duration.ZERO },
    private val tones: () -> List<Tone> = { emptyList() },
) {
    private var lists by mutableStateOf(emptyMap<AmpModel, SongList>())
    private var opened by mutableStateOf(emptyMap<AmpModel, String>())

    /** The untitled song of an amp whose open song is not kept yet. */
    private var drafts by mutableStateOf(emptyMap<AmpModel, Song>())
    private var notices by mutableStateOf(emptyMap<AmpModel, SongNotice>())
    private var playing by mutableStateOf<Job?>(null)
    private var taps = TapTempo()
    private val writes = Mutex()

    /** Whether the song turned the tempo lock on, so it turns it off again when it ends. */
    private var locking = false

    private var unsaved by mutableStateOf<String?>(null)

    /** Why the open song cannot be read, or why a change could not be saved. */
    val failure: String? get() = openKept()?.problem ?: unsaved

    /** The clicks of the count-in still to come, this one among them, or null while not counting in. */
    var countdown by mutableStateOf<Int?>(null)
        private set

    /** Where the playing song stands, or null while none plays. */
    var position by mutableStateOf<Song.Position?>(null)
        private set

    init {
        AmpModels.all.forEach { amp -> scope.launch { writes.withLock { refresh(amp) } } }
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

    /** Whether the song can be changed: not while it counts in or plays, nor when its file could not be read. */
    val editable: Boolean get() = playing == null && openKept().let { it == null || it.song != null }

    /** Whether another song can be opened: not while one counts in or plays. */
    val choosable: Boolean get() = playing == null

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
        taps = TapTempo()
        queue { store.open(amp, kept.name) }
    }

    /** Makes and opens a song called [name], at the starting tempo with the count-in on and no steps. */
    fun create(name: String) {
        val amp = panel.amp
        val trimmed = name.trim()
        if (!choosable || trimmed.isEmpty() || taken(trimmed)) return
        val song = Song(amp, name = trimmed)
        keep(song, replacing = null)
        drafts = drafts - amp
        queue { store.save(song).mapCatching { store.open(amp, trimmed).getOrThrow() } }
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
        val list = lists[amp] ?: return
        val song = list.find(name)?.song ?: return
        val copy = song.copy(name = list.apart(song.name.ifBlank { UNTITLED_COPY }))
        val position = list.songs.indexOfFirst { it.name == song.name } + 2
        listed(amp) { store.save(copy).mapCatching { store.move(amp, copy.name, position).getOrThrow() } }
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
                    stream.use(::capped)?.decodeToString()
                } catch (e: IOException) {
                    return@listed Result.failure(e)
                } ?: return@listed Result.failure(IOException("larger than a song file can be"))
            val other = SongFile.ampOf(text)
            if (other != null && other != amp.id) {
                return@listed Result.success(SongNotice.OtherAmp(AmpModels.byId(other)?.name ?: other))
            }
            val song = SongFile.read(text).getOrElse { return@listed Result.failure(it) }
            val list = store.list(amp).getOrElse { return@listed Result.failure(it) }
            val name = list.apart(song.name.ifBlank { SongStore.CARRIED })
            store.save(song.copy(name = name)).map {
                if (name != song.name &&
                    song.name.isNotBlank()
                ) {
                    SongNotice.ImportedAs(song.name, name)
                } else {
                    null
                }
            }
        }
    }

    fun dismiss() {
        notices = notices - panel.amp
    }

    fun tempo(bpm: Int) = change { it.copy(tempo = bpm.coerceIn(Song.TEMPOS)) }

    fun countIn(on: Boolean) = change { it.copy(countIn = on) }

    /** A tap on the song's beat at [at], from any fixed point; from the second tap on, the tempo follows the taps. */
    fun tap(at: Duration) {
        taps = taps.after(at)
        val beat = taps.beat ?: return
        tempo(Tempo.beatsPerMinute(beat.inWholeMilliseconds.toInt().coerceAtLeast(1)))
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
        if (playing != null || song.steps.isEmpty() || !panel.linked) return
        val time = song.amp.controls.firstOrNull(Control::timed)
        playing =
            scope.launch {
                val counts = if (song.countIn) Song.COUNT_IN_BEATS else 0
                if (counts > 0) countdown = counts
                val late = if (counts > 0) lag().coerceIn(Duration.ZERO, MOST_LAG) else Duration.ZERO
                // Measured from the first step, so the count-in runs in negative time.
                val started = TimeSource.Monotonic.markNow() + song.lead + late
                repeat(counts) {
                    countdown = counts - it
                    click()
                    delay(song.beat * (it + 1) - song.lead - late - started.elapsedNow())
                }
                delay(-started.elapsedNow())
                countdown = null
                var cued = -1
                while (true) {
                    val elapsed = started.elapsedNow().coerceAtLeast(Duration.ZERO)
                    val at = song.at(elapsed) ?: break
                    if (at.step != cued) {
                        if (cued < 0 && time != null && panel.lock == null) {
                            panel.lock(time, on = true)
                            locking = true
                        }
                        panel.cue(song.cue(at.step, ::tone))
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
        countdown = null
        position = null
        playing = null
    }

    private fun change(how: (Song) -> Song) {
        if (!editable) return
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

        /** The most a click is played ahead of its beat; a sound output slower than this is mismeasured. */
        private val MOST_LAG = 500.milliseconds

        /** The name a copy of an untitled song is set apart from. */
        private const val UNTITLED_COPY = "Untitled song"

        // A song file is well under a kilobyte; anything past this is not one.
        private const val MAX_FILE_BYTES = 64 * 1024

        /** The bytes of [stream], or null when it runs past [MAX_FILE_BYTES]. */
        private fun capped(stream: InputStream): ByteArray? {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) return bytes.toByteArray()
                bytes.write(buffer, 0, read)
                if (bytes.size() > MAX_FILE_BYTES) return null
            }
        }
    }
}
