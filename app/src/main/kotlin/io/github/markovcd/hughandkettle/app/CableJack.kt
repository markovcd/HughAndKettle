package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where USB MIDI cables plugged into the phone are found and opened: Android's MIDI service, or a test's stand-in. */
interface CableJack {
    /** The cables plugged in now, as they come and go. */
    val cables: StateFlow<List<Cable>>

    /**
     * Opens [cable] and answers [onOpened] once, with the open cable or why not. Messages the cable
     * hears from the amp go to [onMessage]; [onLost] fires once if it is unplugged while open.
     */
    fun open(
        cable: Cable,
        onMessage: (MidiMessage) -> Unit,
        onLost: (reason: String) -> Unit,
        onOpened: (Result<CableLink>) -> Unit,
    )

    /** A phone with no USB MIDI. */
    object None : CableJack {
        override val cables: StateFlow<List<Cable>> = MutableStateFlow(emptyList<Cable>()).asStateFlow()

        override fun open(
            cable: Cable,
            onMessage: (MidiMessage) -> Unit,
            onLost: (reason: String) -> Unit,
            onOpened: (Result<CableLink>) -> Unit,
        ) = onOpened(Result.failure(IllegalStateException("this phone has no USB MIDI")))
    }
}
