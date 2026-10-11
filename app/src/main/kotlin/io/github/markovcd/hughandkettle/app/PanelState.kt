package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

/** What the panel shows and does: the link's state and the last value sent or heard per control. */
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
    val values = mutableStateMapOf<String, Int>()

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
    ) {
        values[control.id] = value
        if (link !is LinkState.Connected) return
        scope.launch(Dispatchers.IO) {
            holder.send(control.set(value)).onFailure { failure = PanelFailure.SendFailed(it.message ?: "link lost") }
        }
    }

    private fun heard(message: MidiMessage) {
        if (message !is ControlChange) return
        val control = amp.controls.firstOrNull { it.controller == message.controller } ?: return
        values[control.id] = message.value
    }
}
