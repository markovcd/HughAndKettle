package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.tab_amp
import io.github.markovcd.hughandkettle.ui.tab_fx
import io.github.markovcd.hughandkettle.ui.tones
import org.jetbrains.compose.resources.StringResource

/** The panel's tabs along the bottom. Tests find each by `tab:<id>`. */
enum class PanelTab(
    val id: String,
    val label: StringResource,
) {
    Amp("amp", Res.string.tab_amp),
    Fx("fx", Res.string.tab_fx),
    Tones("tones", Res.string.tones),
}
