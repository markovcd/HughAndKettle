package io.github.markovcd.hughandkettle.app

import android.app.Application
import android.net.ConnectivityManager
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Holds what outlives a screen: the route over WiFi and the network it is, the link to the WMI-1 or the cable,
 * the service that keeps it while it is up, the click a song counts in with, and what the app keeps on the phone.
 */
class HughAndKettleApplication : Application() {
    val link: LinkHolder by lazy { LinkHolder(MidiCableJack(this)) }
    val home: AppHome by lazy { AppHome(filesDir.toPath(), settings) }
    val click: ClickSound by lazy { ClickSound() }
    val wifi: WifiRoute by lazy { WifiRoute(checkNotNull(getSystemService(ConnectivityManager::class.java))) }

    override fun onCreate() {
        super.onCreate()
        wifi.start()
        MainScope().launch {
            combine(link.state, link.cable) { state, cable -> state != LinkState.Idle || cable.active }
                .distinctUntilChanged()
                .filter { it }
                .collect { LinkService.start(this@HughAndKettleApplication) }
        }
        MainScope().launch {
            link.state.filterIsInstance<LinkState.Connected>().collect { home.lastWmi.remember(it.address) }
        }
    }
}
