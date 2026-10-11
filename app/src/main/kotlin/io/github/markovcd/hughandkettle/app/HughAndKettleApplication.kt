package io.github.markovcd.hughandkettle.app

import android.app.Application
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/** Holds what outlives a screen: the link to the WMI-1, and the service that keeps it while it is up. */
class HughAndKettleApplication : Application() {
    val link: LinkHolder by lazy { LinkHolder() }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            link.state
                .map { it != LinkState.Idle }
                .distinctUntilChanged()
                .filter { it }
                .collect { LinkService.start(this@HughAndKettleApplication) }
        }
    }
}
