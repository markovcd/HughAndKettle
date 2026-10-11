package io.github.markovcd.hughandkettle.app

import android.app.Application

/** Holds what outlives a screen: the link to the WMI-1. */
class HughAndKettleApplication : Application() {
    val link: LinkHolder by lazy { LinkHolder() }
}
