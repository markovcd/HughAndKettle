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

/** The app's one link to a WMI-1, as flows the panel and the service both watch. */
class LinkHolder : Closeable {
    private val states = MutableStateFlow<LinkState>(LinkState.Idle)
    private val messages = MutableSharedFlow<MidiMessage>(extraBufferCapacity = HEARD_BUFFER)
    private val keeper = LinkKeeper(onState = { states.value = it }, onMessage = { messages.tryEmit(it) })

    val state: StateFlow<LinkState> = states.asStateFlow()
    val heard: SharedFlow<MidiMessage> = messages.asSharedFlow()

    fun connect(address: WmiAddress) = keeper.connect(address)

    fun stop() = keeper.stop()

    fun send(message: MidiMessage): Result<Unit> = keeper.send(message)

    override fun close() = keeper.close()

    private companion object {
        // A burst of knob moves from the amp; anything beyond is dropped rather than blocking the link.
        const val HEARD_BUFFER = 256
    }
}
