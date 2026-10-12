package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** What the panel shows and does: the link's state and the amp's state, as sent and heard. */
class PanelState(
    val amp: AmpModel,
    private val holder: LinkHolder,
    private val scope: CoroutineScope,
    lastWmi: Flow<WmiAddress?>,
) {
    var host by mutableStateOf("")
    var port by mutableStateOf(WmiDefaults.MIDI_PORT.toString())
    var link by mutableStateOf(holder.state.value)
        private set
    var failure by mutableStateOf<PanelFailure?>(null)
        private set
    var state by mutableStateOf(AmpState(amp))
        private set

    /** The last box a link reached, offered first. */
    var last by mutableStateOf<WmiAddress?>(null)
        private set

    init {
        scope.launch { holder.state.collect { link = it } }
        scope.launch { holder.heard.collect(::heard) }
        scope.launch {
            lastWmi.collect {
                last = it
                if (it != null && host.isBlank()) {
                    host = it.host
                    port = it.port.toString()
                }
            }
        }
    }

    /** The fields are editable only while there is nothing to protect. */
    val idle: Boolean get() = link == LinkState.Idle

    fun connect() {
        val address = WmiAddress.parse("${host.trim()}:${port.trim()}")
        if (address == null) {
            failure = PanelFailure.NeedsAddress
            return
        }
        connect(address)
    }

    /** Connects to [address], showing it in the fields. */
    fun connect(address: WmiAddress) {
        host = address.host
        port = address.port.toString()
        failure = null
        holder.connect(address)
    }

    fun disconnect() {
        failure = null
        holder.stop()
    }

    fun set(
        control: Control,
        value: Int,
    ) = send(control.set(value))

    /** Recalls [preset], counted from 1 as the amp shows it. */
    fun recall(preset: Int) = send(amp.recall(preset))

    /** Sends every value [tone] holds, in order, so a channel switch lands before the knobs it carries. */
    fun recall(tone: Tone) = send(tone.recall())

    private fun send(message: MidiMessage) = send(listOf(message))

    private fun send(messages: List<MidiMessage>) {
        state = messages.fold(state) { before, message -> before.after(message) }
        if (link !is LinkState.Connected) return
        scope.launch(Dispatchers.IO) {
            for (message in messages) {
                holder.send(message).onFailure {
                    failure = PanelFailure.SendFailed(it.message ?: "link lost")
                    return@launch
                }
            }
        }
    }

    private fun heard(message: MidiMessage) {
        state = state.after(message)
    }
}
