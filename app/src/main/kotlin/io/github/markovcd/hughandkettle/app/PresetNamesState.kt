package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.PresetNames
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The names given on the phone to the presets of every amp model, and naming one of the panel's amp.
 * The folder is read and written off the main thread; a file that cannot be read names nothing and
 * says why in [failure], and is left as it is.
 */
class PresetNamesState(
    private val store: PresetNameStore,
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) {
    private var all by mutableStateOf(emptyMap<AmpModel, PresetNames>())

    var failure by mutableStateOf<String?>(null)
        private set

    init {
        AmpModels.all.forEach { scope.launch { refresh(it) } }
    }

    /** The names of the panel's amp's presets. */
    val names: PresetNames get() = all[panel.amp] ?: PresetNames(panel.amp)

    /** Names [preset] of the panel's amp [to], or takes its name away when [to] is blank. */
    fun name(
        preset: Int,
        to: String,
    ) {
        val amp = panel.amp
        scope.launch {
            withContext(io) { store.name(amp, preset, to) }.onFailure { failure = it.message }
            refresh(amp)
        }
    }

    private suspend fun refresh(amp: AmpModel) {
        val read = withContext(io) { store.names(amp) }
        read.onFailure { failure = it.message }
        all = all + (amp to read.getOrElse { PresetNames(amp) })
    }
}
