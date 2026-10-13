package io.github.markovcd.hughandkettle.app

import android.app.Application
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch

/**
 * Holds what outlives a screen: the link to the WMI-1 or the cable, the service that keeps it while it is up, the
 * last box it reached, the amp picked for each box, the folder of tones, and the theme picked.
 */
class HughAndKettleApplication : Application() {
    val link: LinkHolder by lazy { LinkHolder(MidiCableJack(this)) }
    val lastWmi: LastWmiStore by lazy { LastWmiStore(this) }
    val tones: ToneStore by lazy { ToneStore(filesDir.toPath().resolve("tones")) }
    val theme: ThemeStore by lazy { ThemeStore(this) }
    val amps: AmpChoiceStore by lazy { AmpChoiceStore(this) }

    override fun onCreate() {
        super.onCreate()
        MainScope().launch {
            combine(link.state, link.cable) { state, cable -> state != LinkState.Idle || cable.active }
                .distinctUntilChanged()
                .filter { it }
                .collect { LinkService.start(this@HughAndKettleApplication) }
        }
        MainScope().launch {
            link.state.filterIsInstance<LinkState.Connected>().collect { lastWmi.remember(it.address) }
        }
    }
}
