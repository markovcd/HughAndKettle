package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import io.github.markovcd.hughandkettle.amp.CappedRead
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.PresetReading
import io.github.markovcd.hughandkettle.amp.PresetWriting
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneImport
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.amp.TonesRead
import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The tones kept on the phone, in their order, and what the panel can do with them: save the amp's
 * state as one, read the amp's presets into them, recall, rename, move, delete, export and import.
 * The folder is read and written off the main thread; the list is replaced whole after every change.
 * Reading and writing listen to what the amp says through [heard]; a tone read is named after [names], and a
 * preset written is handed to [named] with the tone's name. A tone renamed is handed to [renamed], so the songs
 * that step on it follow.
 */
class ToneShelf(
    private val store: ToneStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val heard: Flow<MidiMessage> = emptyFlow(),
    private val names: () -> PresetNames = { PresetNames(panel.amp) },
    private val renamed: (Tone, String) -> Unit = { _, _ -> },
    private val named: (Int, String) -> Unit = { _, _ -> },
) {
    var tones by mutableStateOf<List<Tone>>(emptyList())
        private set
    var notice by mutableStateOf<ToneNotice?>(null)
        private set

    /** The reading of the amp's presets under way, or null. */
    var reading by mutableStateOf<PresetReading?>(null)
        private set
    private var stopping = false

    init {
        refresh()
    }

    /** Whether there is anything to save: a tone holds only what the panel knows. */
    val canSave: Boolean get() = panel.state.values.isNotEmpty()

    /** Whether [tone] is for the amp on the panel, and so can be recalled onto it. */
    fun fits(tone: Tone): Boolean = tone.amp == panel.amp

    /** The pinned tones for the amp on the panel, in the shelf's order: what the header's − and + step through. */
    val pinned: List<Tone> get() = tones.filter { it.pinned && fits(it) }

    fun pin(
        tone: Tone,
        on: Boolean,
    ) = change { store.pin(tone.name, on) }

    fun save(name: String) {
        if (name.isNotBlank() && canSave) change { store.save(panel.tone(name.trim())) }
    }

    /** Whether the amp's presets can be read now: it is linked, and no reading is under way. */
    val canRead: Boolean get() = panel.linked && reading == null

    /**
     * Recalls every preset of the panel's amp in turn and keeps what the amp reports of each as a tone
     * named after it, in place of a tone of the same name. The preset the amp was on is recalled after.
     */
    fun readPresets() {
        if (!canRead) return
        val back = panel.state.preset
        stopping = false
        reading = PresetReading.of(names())
        scope.launch {
            val inbox = Channel<MidiMessage>(Channel.UNLIMITED)
            val listening = launch(start = CoroutineStart.UNDISPATCHED) { heard.collect { inbox.send(it) } }
            var now = reading ?: return@launch
            while (!stopping && panel.linked) {
                val preset = now.preset ?: break
                panel.recall(preset)
                now = withTimeoutOrNull(PER_PRESET_MILLIS) {
                    var next = now
                    while (next.preset == preset) next = next.after(inbox.receive())
                    next
                } ?: now.skip()
                reading = now
            }
            listening.cancel()
            back?.let(panel::recall)
            val read = now
            reading = null
            change { store.save(read.tones, replace = true).map { ToneNotice.Read(read.tones.size, read.missed) } }
        }
    }

    /** Ends a reading after the preset being read, keeping the tones read so far. */
    fun stopReading() {
        stopping = true
    }

    /** The names of the amp's presets, which a write is picked from. */
    val presetNames: PresetNames get() = names()

    /** The preset the amp is on, if known. */
    val preset: Int? get() = panel.state.preset

    /** The write of a tone into a preset under way or just ended, or null. */
    var writing by mutableStateOf<PresetWriting?>(null)
        private set
    private var writes: Channel<WriteEvent>? = null

    /** Whether [tone] can be written into a preset now: it fits the linked amp, and nothing else is reading or writing. */
    fun canWrite(tone: Tone): Boolean = panel.linked && fits(tone) && reading == null && writing?.over != false

    /**
     * Writes [tone] into [preset]: the preset is kept first as a backup tone, then the tone is sent and the amp's
     * STORE answered with the preset's program change. The lock and the trim hold off until it ends.
     */
    fun write(
        tone: Tone,
        preset: Int,
    ) {
        if (!canWrite(tone)) return
        drive(PresetWriting.start(tone, preset))
    }

    /** Calls off the write while STORE is still awaited. */
    fun cancelWrite() {
        writes?.trySend(WriteEvent.Cancel)
    }

    /** Recalls the preset of a write left unknown, to see whether it holds the tone. */
    fun checkWrite() {
        val now = writing ?: return
        if (now.stage == PresetWriting.Stage.Unsure && panel.linked) drive(now.check())
    }

    /** Puts away a write that has ended. */
    fun closeWrite() {
        if (writing?.over == true) writing = null
    }

    private fun drive(started: Pair<PresetWriting, List<MidiMessage>>) {
        val (start, first) = started
        writing = start
        panel.holding = true
        val inbox = Channel<WriteEvent>(Channel.UNLIMITED)
        writes = inbox
        scope.launch {
            val listening =
                launch(start = CoroutineStart.UNDISPATCHED) { heard.collect { inbox.send(WriteEvent.Heard(it)) } }
            val watching = launch { snapshotFlow { panel.linked }.collect { if (!it) inbox.send(WriteEvent.Lost) } }
            var now = start
            var timer = late(inbox, now)
            panel.sendAsIs(first)
            while (!now.over) {
                val next =
                    when (val event = inbox.receive()) {
                        is WriteEvent.Heard -> {
                            val (after, send) = now.after(event.message)
                            panel.sendAsIs(send)
                            after
                        }

                        WriteEvent.Cancel -> {
                            now.cancel()
                        }

                        WriteEvent.Lost -> {
                            now.lost()
                        }

                        is WriteEvent.Late -> {
                            if (event.stage == now.stage) now.gaveUp() else now
                        }
                    }
                if (next.stage != now.stage) {
                    timer.cancel()
                    timer = late(inbox, next)
                    if (next.stage == PresetWriting.Stage.Waiting) keep(next.backup!!)
                    if (next.stage is PresetWriting.Stage.Stored) named(next.preset, next.tone.name)
                }
                now = next
                writing = now
            }
            timer.cancel()
            listening.cancel()
            watching.cancel()
            writes = null
            panel.holding = false
        }
    }

    /** A timer that tells [inbox] when [writing]'s step outlasts its limit. */
    private fun CoroutineScope.late(
        inbox: Channel<WriteEvent>,
        writing: PresetWriting,
    ): Job =
        launch {
            val limit = writing.limit ?: return@launch
            delay(limit)
            inbox.send(WriteEvent.Late(writing.stage))
        }

    /** Saves [backup] named apart from the tones on the shelf. */
    private fun keep(backup: Tone) =
        change {
            val apart = TonesRead(listOf(backup), emptyList()).namedApartFrom(store.list().tones.map { it.name })
            val kept = apart.tones.single()
            store.save(kept).map { ToneNotice.BackedUp(kept.name) }
        }

    /** What the write's loop takes in, in the order it came. */
    private sealed interface WriteEvent {
        data class Heard(
            val message: MidiMessage,
        ) : WriteEvent

        data object Cancel : WriteEvent

        data object Lost : WriteEvent

        data class Late(
            val stage: PresetWriting.Stage,
        ) : WriteEvent
    }

    /** The name of the tone last recalled from the shelf, which stepping moves on from. */
    var recalled by mutableStateOf<String?>(null)
        private set

    /** The tone last recalled, while it is on the shelf and for the amp on the panel. */
    val loaded: Tone? get() = tones.firstOrNull { it.name == recalled && fits(it) }

    fun recall(tone: Tone) {
        if (!fits(tone)) return
        panel.recall(tone)
        recalled = tone.name
    }

    /**
     * Recalls the pinned tone [by] places on from the one last recalled, down the shelf for positive, and
     * returns it. From none, or from a tone not pinned, it starts at the first or the last; past either end
     * it wraps around. With none pinned it recalls nothing.
     */
    fun step(by: Int): Tone? {
        val pinned = pinned
        if (pinned.isEmpty()) return null
        val at = pinned.indexOfFirst { it.name == recalled }
        val next =
            when {
                at >= 0 -> at + by
                by > 0 -> by - 1
                else -> pinned.size + by
            }.mod(pinned.size)
        return pinned[next].also(::recall)
    }

    fun rename(
        tone: Tone,
        to: String,
    ) {
        val name = to.trim()
        change {
            store.rename(tone.name, name).onSuccess {
                renamed(tone, name)
                if (recalled == tone.name) recalled = name
            }
        }
    }

    fun delete(tone: Tone) = change { store.delete(tone.name) }

    /** Moves [tone] [by] places, up for negative; past either end it stays where it is. */
    fun move(
        tone: Tone,
        by: Int,
    ) {
        val position = tones.indexOf(tone) + 1 + by
        if (position in 1..tones.size) change { store.move(tone.name, position) }
    }

    /** Writes [tone] in the tone file format to what [open] opens. */
    fun export(
        tone: Tone,
        open: () -> OutputStream?,
    ) = change {
        try {
            val stream = open() ?: return@change Result.failure(IOException("nowhere to write"))
            stream.use { it.write(ToneFile.write(tone).toByteArray()) }
            Result.success(Unit)
        } catch (e: IOException) {
            Result.failure(e)
        }
    }

    /**
     * Reads a tone file, or the GrandMeister app's preset file, from what [open] opens and keeps its tones.
     * A tone file's name already on the shelf is refused; a preset file's tones are named apart from the shelf's.
     */
    fun import(open: () -> InputStream?) =
        change {
            val bytes =
                try {
                    val stream = open() ?: return@change Result.failure(IOException("nothing to read"))
                    stream.use { CappedRead.bytes(it, ToneImport.MOST_BYTES) }
                } catch (e: IOException) {
                    return@change Result.failure(e)
                } ?: return@change Result.failure(IOException("larger than a tone file can be"))
            val read = ToneImport.read(bytes).getOrElse { return@change Result.failure(it) }
            if (ToneImport.isPresetFile(bytes)) {
                val apart = read.namedApartFrom(store.list().tones.map { it.name })
                store.save(apart.tones).map { ToneNotice.ImportedPresets(apart.tones.size, read.skipped) }
            } else {
                val tone = read.tones.single()
                store.save(tone).map { ToneNotice.Imported(tone.name, read.skipped) }
            }
        }

    fun dismiss() {
        notice = null
    }

    private fun refresh() =
        scope.launch {
            tones = withContext(io) { store.list().tones }
        }

    /** Runs [action] off the main thread, then shows the list as it now is and what came of it. */
    private fun change(action: () -> Result<Any>) {
        scope.launch {
            val result = withContext(io) { action() }
            notice = result.fold({ it as? ToneNotice }, { ToneNotice.Failed(it.message ?: "${it::class.simpleName}") })
            tones = withContext(io) { store.list().tones }
        }
    }

    private companion object {
        // The amp answers a recall in tens of milliseconds over the WMI-1; this leaves room for a busy network.
        const val PER_PRESET_MILLIS = 2000L
    }
}
