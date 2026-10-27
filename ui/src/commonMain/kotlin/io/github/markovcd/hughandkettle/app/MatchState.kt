package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.ChordListen
import io.github.markovcd.hughandkettle.amp.LevelFrame
import io.github.markovcd.hughandkettle.amp.LoudnessCorrectionStore
import io.github.markovcd.hughandkettle.amp.LoudnessCorrections
import io.github.markovcd.hughandkettle.amp.LoudnessMatch
import io.github.markovcd.hughandkettle.amp.Microphone
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.Song
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The loudness match: the soundcheck's set-up, the run through the microphone, its summary, and the corrections
 * kept for every amp model, which the panel adds to each recall. The folder is read and written off the main
 * thread; a file that cannot be read corrects nothing and says why in [failure], and is left as it is.
 */
class MatchState(
    private val store: LoudnessCorrectionStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val names: () -> PresetNames,
    private val song: () -> Song,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val settle: Duration = 1.5.seconds,
    private val answerLimit: Duration = 3.seconds,
) {
    private var kept by mutableStateOf(emptyMap<AmpModel, LoudnessCorrections>())

    var failure by mutableStateOf<String?>(null)
        private set

    /** True while the set-up sheet is shown, before the soundcheck starts. */
    var settingUp by mutableStateOf(false)
        private set

    /** Whether the set-up offers every named preset rather than the open song's. */
    var everyNamed by mutableStateOf(false)
        private set

    var picked by mutableStateOf(emptySet<Int>())
        private set

    var reference by mutableStateOf<Int?>(null)
        private set

    /** The soundcheck running or, once done, summed up; null while there is none. */
    var match by mutableStateOf<LoudnessMatch?>(null)
        private set

    /** The microphone's level now, in dB below full scale, while a soundcheck listens. */
    var level by mutableStateOf<Double?>(null)
        private set

    /** The summary's corrections by preset, cleared rows at 0, until kept or discarded. */
    var summary by mutableStateOf(emptyMap<Int, Int>())
        private set

    /** True when the microphone was refused, until the next start. */
    var denied by mutableStateOf(false)
        private set

    private var running: Job? = null

    init {
        AmpModels.all.forEach { scope.launch { refresh(it) } }
    }

    /** The corrections kept for the panel's amp. */
    val corrections: LoudnessCorrections get() = kept[panel.amp] ?: LoudnessCorrections(panel.amp)

    /** Whether the panel's amp has a volume to correct. */
    val available: Boolean get() = panel.trim != null

    /** Whether the sheet is up: setting up, running or summing up. */
    val shown: Boolean get() = settingUp || match != null

    /** The open song's name, when its presets are offered. */
    val songName: String? get() = song().name.takeIf { !everyNamed && songPresets.isNotEmpty() }

    /** The presets the set-up offers: the open song's, or every named one, else the one playing now. */
    val offered: List<Int>
        get() {
            val named = names().names.keys.sorted()
            val offered = if (everyNamed || songPresets.isEmpty()) named else songPresets
            return offered.ifEmpty { listOfNotNull(panel.state.preset) }
        }

    private val songPresets: List<Int> get() = song().steps.mapNotNull { it.preset }.distinct()

    /** Opens the set-up on the open song's presets, all picked, the preset playing now the reference if it is one. */
    fun open() {
        if (!available) return
        failure = null
        denied = false
        everyNamed = false
        match = null
        summary = emptyMap()
        settingUp = true
        offer()
    }

    /** Offers every named preset, or the open song's again. */
    fun pickEveryNamed(on: Boolean) {
        everyNamed = on
        offer()
    }

    fun pick(
        preset: Int,
        on: Boolean,
    ) {
        picked = if (on) picked + preset else picked - preset
        if (!on && reference == preset) reference = picked.minOrNull()
        if (on && reference == null) reference = preset
    }

    /** Makes [preset] the reference, picking it if it was not. */
    fun refer(preset: Int) {
        picked = picked + preset
        reference = preset
    }

    /** Whether the soundcheck can start: a link, a reference and at least one other preset. */
    val startable: Boolean get() = panel.linked && reference != null && picked.size >= 2

    /** Asks for the microphone through [access] and, once allowed, starts the soundcheck. */
    fun start(access: MicrophoneAccess) {
        if (!startable) return
        denied = false
        access.ask { allowed ->
            if (allowed) start(access.microphone) else denied = true
        }
    }

    /** Starts the soundcheck of the picked presets, listening through [microphone]. */
    fun start(microphone: Microphone) {
        val reference = reference ?: return
        if (!startable) return
        val order = offered.filter { it in picked } + picked.filter { it !in offered }.sorted()
        settingUp = false
        failure = null
        match = LoudnessMatch.of(order, reference)
        running?.cancel()
        running = scope.launch { listen(microphone) }
    }

    /** Leaves the preset being matched as it is. */
    fun skip() {
        match = match?.skip()
    }

    /** Ends the soundcheck here, on its summary. */
    fun stop() {
        match = match?.stop()
    }

    /** Takes [preset]'s correction out of the summary, or out of those kept when there is no summary. */
    fun clear(preset: Int) {
        if (match != null) {
            summary = summary + (preset to 0)
        } else {
            keep(corrections.without(preset))
        }
    }

    /** Takes every correction kept for the panel's amp away. */
    fun clearAll() = keep(LoudnessCorrections(panel.amp))

    /** Keeps the summary's corrections in place of the old ones for the same presets, and closes the sheet. */
    fun keep() {
        val done = match ?: return
        if (done.stage != LoudnessMatch.Stage.Done) return
        keep(corrections.with(summary))
        close()
    }

    /** Closes the sheet, stopping a soundcheck and keeping nothing from it. */
    fun close() {
        running?.cancel()
        running = null
        settingUp = false
        match = null
        summary = emptyMap()
        level = null
    }

    private fun offer() {
        val offered = offered
        picked = offered.toSet()
        reference = panel.state.preset?.takeIf { it in offered } ?: offered.firstOrNull()
    }

    private fun keep(corrections: LoudnessCorrections) {
        scope.launch {
            withContext(io) { store.keep(corrections) }.onFailure {
                failure = it.message
                return@launch
            }
            kept = kept + (corrections.amp to corrections)
            panel.correct(corrections)
        }
    }

    private suspend fun refresh(amp: AmpModel) {
        val read = withContext(io) { store.corrections(amp) }
        read.onFailure { failure = it.message }
        val corrections = read.getOrElse { LoudnessCorrections(amp) }
        kept = kept + (amp to corrections)
        panel.correct(corrections)
    }

    /** Runs the soundcheck through [microphone], its frames read off the main thread, until it is done or closed. */
    private suspend fun listen(microphone: Microphone) {
        val input =
            withContext(io) { microphone.open() }.getOrElse {
                failure = it.message
                match = null
                return
            }
        val frames = Channel<LevelFrame>(FRAMES_KEPT, BufferOverflow.DROP_OLDEST)
        // A read blocks until the frame is heard, so it gets a thread of its own whatever [io] is.
        val reader =
            scope.launch(Dispatchers.IO) {
                while (true) {
                    val frame =
                        input.read().getOrElse {
                            frames.close(it)
                            break
                        }
                    frames.send(frame)
                }
            }
        try {
            run(frames)
        } finally {
            // Closing unblocks the reader's read; a cancelled soundcheck cannot switch threads to do it.
            input.close()
            reader.cancel()
            level = null
        }
    }

    private suspend fun run(frames: ReceiveChannel<LevelFrame>) {
        while (true) {
            val now = match ?: return
            when (now.stage) {
                LoudnessMatch.Stage.Recalling -> {
                    recall(now, frames)
                }

                is LoudnessMatch.Stage.Playing, is LoudnessMatch.Stage.Retry -> {
                    measure(now, frames)
                }

                is LoudnessMatch.Stage.Adjusting -> {
                    withTimeoutOrNull(settle) { for (frame in frames) level = frame.level }
                    drain(frames)
                    if (match === now) match = now.adjusted()
                }

                LoudnessMatch.Stage.Done -> {
                    summary = now.corrections()
                    return
                }
            }
        }
    }

    /** Recalls the preset [now] is at and hands on what the amp plays, or stops the soundcheck if it never answers. */
    private suspend fun recall(
        now: LoudnessMatch,
        frames: ReceiveChannel<LevelFrame>,
    ) {
        val preset = now.at ?: return
        val answered =
            withTimeoutOrNull(answerLimit) {
                panel.blocks.onSubscription { panel.recall(preset) }.first { it.preset == preset }
            }
        val shift = panel.trim?.shift
        val volume = panel.amp.control("volume")?.let { panel.state.values[it.id] }
        if (answered == null || shift == null || volume == null) {
            failure = "The amp did not answer the recall of preset $preset."
            if (match === now) match = now.stop()
            return
        }
        drain(frames)
        if (match === now) match = now.recalled(shift.stored, shift.correction, volume)
    }

    /** Drops the frames heard so far: a chord already gone, or the volume changing. */
    private fun drain(frames: ReceiveChannel<LevelFrame>) {
        do {
            val stale = frames.tryReceive().isSuccess
        } while (stale)
    }

    /** Listens for one chord and sends the volume it calls for, or stops the soundcheck if the microphone stopped. */
    private suspend fun measure(
        now: LoudnessMatch,
        frames: ReceiveChannel<LevelFrame>,
    ) {
        var listen = ChordListen.start()
        for (frame in frames) {
            if (match !== now) return
            level = frame.level
            listen = listen.after(frame)
            val outcome = listen.outcome ?: continue
            val (next, volume) = now.heard(outcome)
            match = next
            val control = panel.amp.control("volume")
            if (volume != null && control != null) panel.set(control, volume)
            return
        }
        failure = "The microphone stopped."
        if (match === now) match = now.stop()
    }

    private companion object {
        // Two seconds of frames: a slow main thread loses the oldest, not the newest.
        const val FRAMES_KEPT = 40
    }
}
