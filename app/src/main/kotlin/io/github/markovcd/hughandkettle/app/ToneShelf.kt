package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneStore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The tones kept on the phone, in their order, and what the panel can do with them: save the amp's
 * state as one, recall, rename, move, delete, export and import. The folder is read and written off
 * the main thread; the list is replaced whole after every change.
 */
class ToneShelf(
    private val store: ToneStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    var tones by mutableStateOf<List<Tone>>(emptyList())
        private set
    var notice by mutableStateOf<ToneNotice?>(null)
        private set

    init {
        refresh()
    }

    /** Whether there is anything to save: a tone holds only what the panel knows. */
    val canSave: Boolean get() = panel.state.values.isNotEmpty()

    /** Whether [tone] is for the amp on the panel, and so can be recalled onto it. */
    fun fits(tone: Tone): Boolean = tone.amp == panel.amp

    fun save(name: String) {
        if (name.isNotBlank() && canSave) change { store.save(Tone.of(name.trim(), panel.state)) }
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

    /** Reads a tone file from what [open] opens and keeps it; a name already on the shelf is refused. */
    fun import(open: () -> InputStream?) =
        change {
            val text =
                try {
                    val stream = open() ?: return@change Result.failure(IOException("nothing to read"))
                    stream.use(::capped)
                } catch (e: IOException) {
                    return@change Result.failure(e)
                } ?: return@change Result.failure(IOException("larger than a tone file can be"))
            val read = ToneFile.read(text).getOrElse { return@change Result.failure(it) }
            store.save(read.tone).map { ToneNotice.Imported(read.tone.name, read.skipped) }
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
        // A tone file of every control of the largest amp is a few kilobytes; anything past this is not one.
        const val MAX_FILE_BYTES = 64 * 1024

        /** The text of [stream], or null when it runs past [MAX_FILE_BYTES]. */
        fun capped(stream: InputStream): String? {
            val bytes = ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val read = stream.read(buffer)
                if (read < 0) return bytes.toString(Charsets.UTF_8.name())
                bytes.write(buffer, 0, read)
                if (bytes.size() > MAX_FILE_BYTES) return null
            }
        }
    }
}
