package io.github.markovcd.hughandkettle.app

import android.app.Application
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Holds what outlives a screen: the link to the WMI-1, the service that keeps it while it is up, and
 * the last box it reached.
 */
class HughAndKettleApplication : Application() {
    val link: LinkHolder by lazy { LinkHolder() }
    val lastWmi: LastWmiStore by lazy { LastWmiStore(this) }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            link.state
                .map { it != LinkState.Idle }
                .distinctUntilChanged()
                .filter { it }
                .collect { LinkService.start(this@HughAndKettleApplication) }
        }
        MainScope().launch {
            link.state.filterIsInstance<LinkState.Connected>().collect { lastWmi.remember(it.address) }
        }
    }
}
