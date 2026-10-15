package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.PresetReading
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneImport
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The tones kept on the phone, in their order, and what the panel can do with them: save the amp's
 * state as one, read the amp's presets into them, recall, rename, move, delete, export and import.
 * The folder is read and written off the main thread; the list is replaced whole after every change.
 * Reading listens to what the amp says through [heard] and names each tone after [names].
 */
class ToneShelf(
    private val store: ToneStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val heard: Flow<MidiMessage> = emptyFlow(),
    private val names: () -> PresetNames = { PresetNames(panel.amp) },
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

    fun recall(tone: Tone) {
        if (fits(tone)) panel.recall(tone)
    }

    fun rename(
        tone: Tone,
        to: String,
    ) = change { store.rename(tone.name, to.trim()) }

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
                    stream.use(::capped)
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

        // A tone file is a few kilobytes and the GrandMeister app's preset file under 32 KB; anything past this is neither.
        const val MAX_FILE_BYTES = 64 * 1024

        /** The bytes of [stream], or null when it runs past [MAX_FILE_BYTES]. */
        fun capped(stream: InputStream): ByteArray? {
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
