package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.LinkKeeper
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.Closeable

/**
 * The app's one way to the amp, over a WMI-1 or a USB MIDI cable plugged into the phone, as flows the
 * panel and the service both watch. Taking one way drops the other.
 */
class LinkHolder(
    private val jack: CableJack = CableJack.None,
) : Closeable {
    private val states = MutableStateFlow<LinkState>(LinkState.Idle)
    private val cableStates = MutableStateFlow<CableState>(CableState.None)
    private val messages = MutableSharedFlow<MidiMessage>(extraBufferCapacity = HEARD_BUFFER)
    private val keeper = LinkKeeper(onState = { states.value = it }, onMessage = { messages.tryEmit(it) })

    @Volatile
    private var open: CableLink? = null

    // Counts cable attempts, so news from one given up on is ignored. Guarded by this.
    private var attempt = 0

    val state: StateFlow<LinkState> = states.asStateFlow()
    val cable: StateFlow<CableState> = cableStates.asStateFlow()
    val cables: StateFlow<List<Cable>> get() = jack.cables
    val heard: SharedFlow<MidiMessage> = messages.asSharedFlow()

    fun connect(address: WmiAddress) {
        drop(CableState.None)
        keeper.connect(address)
    }

    fun connect(cable: Cable) {
        keeper.stop()
        val mine =
            synchronized(this) {
                drop(CableState.Opening(cable))
                attempt
            }
        jack.open(
            cable,
            onMessage = { messages.tryEmit(it) },
            onLost = { lost(mine, cable, it) },
            onOpened = { opened(mine, cable, it) },
        )
    }

    fun stop() {
        keeper.stop()
        drop(CableState.None)
    }

    fun send(message: MidiMessage): Result<Unit> = open?.send(message) ?: keeper.send(message)

    override fun close() {
        drop(CableState.None)
        keeper.close()
    }

    @Synchronized
    private fun drop(next: CableState) {
        attempt++
        open?.close()
        open = null
        cableStates.value = next
    }

    @Synchronized
    private fun opened(
        mine: Int,
        cable: Cable,
        result: Result<CableLink>,
    ) {
        if (mine != attempt) {
            result.onSuccess { it.close() }
            return
        }
        result.fold(
            onSuccess = {
                open = it
                cableStates.value = CableState.Open(cable)
            },
            onFailure = { cableStates.value = CableState.Failed(cable, it.message ?: "cannot open") },
        )
    }

    @Synchronized
    private fun lost(
        mine: Int,
        cable: Cable,
        reason: String,
    ) {
        if (mine == attempt) drop(CableState.Failed(cable, reason))
    }

    private companion object {
        // A burst of knob moves from the amp; anything beyond is dropped rather than blocking the link.
        const val HEARD_BUFFER = 256
    }
}
