package io.github.markovcd.hughandkettle.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiInfo
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sends the app's sockets over WiFi whenever the phone is on one, and says which network that is. A
 * WMI-1's own network has no internet, so Android keeps mobile data as the default and an unbound socket
 * would never reach the box or hear its broadcasts. The app talks to nothing but the box, so the whole
 * process follows WiFi.
 */
class WifiRoute(
    private val connectivity: ConnectivityManager,
) {
    // A request asks for internet unless told otherwise, and the box's network has none to promise.
    private val wifi =
        NetworkRequest
            .Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .removeCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

    private val current = MutableStateFlow<PhoneWifi>(PhoneWifi.Off)
    private var callback: ConnectivityManager.NetworkCallback? = null

    val network: StateFlow<PhoneWifi> = current.asStateFlow()

    fun start() {
        val listening = newCallback()
        callback = listening
        connectivity.registerNetworkCallback(wifi, listening)
    }

    /** Listens afresh, so a name Android kept back before the location grant is heard after it. */
    fun reread() {
        callback?.let(connectivity::unregisterNetworkCallback)
        start()
    }

    // Without the flag Android leaves the network's name out of what it reports.
    private fun newCallback() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Listener(ConnectivityManager.NetworkCallback.FLAG_INCLUDE_LOCATION_INFO)
        } else {
            Listener()
        }

    private inner class Listener : ConnectivityManager.NetworkCallback {
        constructor() : super()

        @RequiresApi(Build.VERSION_CODES.S)
        constructor(flags: Int) : super(flags)

        private var on: Network? = null

        override fun onAvailable(network: Network) {
            connectivity.bindProcessToNetwork(network)
            on = network
            current.value = PhoneWifi.Unnamed
        }

        override fun onCapabilitiesChanged(
            network: Network,
            capabilities: NetworkCapabilities,
        ) {
            val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) ssidOf(capabilities) else null
            current.value = PhoneWifi.of(ssid)
        }

        @RequiresApi(Build.VERSION_CODES.Q)
        private fun ssidOf(capabilities: NetworkCapabilities): String? = (capabilities.transportInfo as? WifiInfo)?.ssid

        override fun onLost(network: Network) {
            if (connectivity.boundNetworkForProcess == network) connectivity.bindProcessToNetwork(null)
            if (on == network) {
                on = null
                current.value = PhoneWifi.Off
            }
        }
    }
}
