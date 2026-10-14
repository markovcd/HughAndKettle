package io.github.markovcd.hughandkettle.app

import androidx.annotation.StringRes
import io.github.markovcd.hughandkettle.R

/** The panel's tabs along the bottom. Tests find each by `tab:<id>`. */
enum class PanelTab(
    val id: String,
    @param:StringRes val label: Int,
) {
    Amp("amp", R.string.tab_amp),
    Fx("fx", R.string.tab_fx),
    Tones("tones", R.string.tones),
}
