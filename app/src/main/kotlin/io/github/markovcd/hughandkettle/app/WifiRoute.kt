package io.github.markovcd.hughandkettle.app

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

/**
 * Sends the app's sockets over WiFi whenever the phone is on one. A WMI-1's own network has no internet,
 * so Android keeps mobile data as the default and an unbound socket would never reach the box or hear
 * its broadcasts. The app talks to nothing but the box, so the whole process follows WiFi.
 */
class WifiRoute(
    private val connectivity: ConnectivityManager,
) {
    private val wifi = NetworkRequest.Builder().addTransportType(NetworkCapabilities.TRANSPORT_WIFI).build()

    private val callback =
        object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                connectivity.bindProcessToNetwork(network)
            }

            override fun onLost(network: Network) {
                if (connectivity.boundNetworkForProcess == network) connectivity.bindProcessToNetwork(null)
            }
        }

    fun start() {
        connectivity.registerNetworkCallback(wifi, callback)
    }
}
