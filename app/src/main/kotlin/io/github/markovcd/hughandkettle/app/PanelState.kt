package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import io.github.markovcd.hughandkettle.wmi.WmiLink
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** What the panel shows and does: the link to the WMI-1 and the last value sent or heard per control. */
class PanelState(
    val amp: AmpModel,
    private val scope: CoroutineScope,
) {
    var host by mutableStateOf("")
    var port by mutableStateOf(WmiDefaults.MIDI_PORT.toString())
    var link by mutableStateOf<WmiLink?>(null)
        private set
    var failure by mutableStateOf<String?>(null)
        private set
    val values = mutableStateMapOf<String, Int>()

    fun connect() {
        val port = port.toIntOrNull()
        if (host.isBlank() || port == null || port !in 1..65535) {
            failure = "Enter the WMI-1 address and a port between 1 and 65535"
            return
        }
        scope.launch {
            val opened =
                withContext(Dispatchers.IO) {
                    WmiLink.open(host.trim(), port, onMessage = ::heard, onClosed = { link = null })
                }
            opened.onSuccess {
                link = it
                failure = null
            }
            opened.onFailure { failure = it.message ?: "Cannot reach $host:$port" }
        }
    }

    fun disconnect() {
        link?.close()
        link = null
    }

    fun set(
        control: Control,
        value: Int,
    ) {
        values[control.id] = value
        val link = link ?: return
        scope.launch(Dispatchers.IO) {
            link.send(control.set(value)).onFailure { failure = it.message ?: "Send failed" }
        }
    }

    private fun heard(message: MidiMessage) {
        if (message !is ControlChange) return
        val control = amp.controls.firstOrNull { it.controller == message.controller } ?: return
        values[control.id] = message.value
    }
}
