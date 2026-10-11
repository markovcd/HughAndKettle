package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.wmi.WmiBroadcast
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext
import kotlin.time.Duration.Companion.seconds

/** The WMI-1s heard broadcasting on the network, newest broadcast per box, in the order first heard. */
class DiscoveryState(
    private val discovery: WmiDiscovery = WmiDiscovery(),
) {
    var found by mutableStateOf<List<WmiBroadcast>>(emptyList())
        private set
    var failure by mutableStateOf<String?>(null)
        private set

    /** Listens until cancelled. */
    suspend fun listen() =
        withContext(Dispatchers.IO) {
            while (true) {
                // Short windows, so cancelling is noticed within one.
                discovery
                    .listen(1.seconds) { heard(it) }
                    .onSuccess { failure = null }
                    .onFailure { failure = it.message ?: it.javaClass.simpleName }
                coroutineContext.ensureActive()
            }
        }

    private fun heard(broadcast: WmiBroadcast): Boolean {
        val key = broadcast.key()
        val list = found
        val at = list.indexOfFirst { it.key() == key }
        found = if (at < 0) list + broadcast else list.toMutableList().also { it[at] = broadcast }
        return true
    }

    private fun WmiBroadcast.key(): String = "${address.hostAddress}:$midiPort"
}
