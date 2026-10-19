package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Section

/**
 * What tops the panel on every layout: a bar of the preset, the link's glyph, the header's switches and then
 * [end]; the switching under it; and the link card while there is no link.
 */
@Composable
fun PanelTop(
    panel: PanelState,
    discovery: DiscoveryState,
    presetNames: PresetNamesState,
    end: @Composable RowScope.() -> Unit = {},
) {
    val enabled = panel.linked
    val dim = Modifier.dimmedUnless(enabled)
    Column {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                PresetRow(panel, presetNames, enabled, Modifier.widthIn(max = PRESET_WIDTH).then(dim))
            }
            LinkButton(panel, discovery)
            panel.amp.controls.filter { it.section == Section.Header }.forEach {
                HeaderSwitch(it, panel, enabled, dim)
            }
            end()
        }
        SwitchingStrip(panel, enabled, dim)
        if (panel.idle) LinkCard(panel, discovery, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** With no link there is nothing to turn: the controls dim to 38%, and the link card keeps full contrast. */
fun Modifier.dimmedUnless(linked: Boolean): Modifier = alpha(if (linked) 1f else DIMMED)

private const val DIMMED = 0.38f
private val PRESET_WIDTH = 280.dp
