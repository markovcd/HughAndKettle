package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp

/**
 * What tops the panel on every layout: the [SwitcherRow] with its [menu]; the switching under it; and the link
 * card while there is no link.
 */
@Composable
fun PanelTop(
    panel: PanelState,
    discovery: DiscoveryState,
    presetNames: PresetNamesState,
    shelf: ToneShelf,
    songs: SongState,
    mode: SwitcherMode,
    onTones: () -> Unit,
    menu: (@Composable ColumnScope.(close: () -> Unit, link: () -> Unit) -> Unit)?,
) {
    val enabled = panel.linked
    val dim = Modifier.dimmedUnless(enabled)
    Column {
        SwitcherRow(panel, discovery, presetNames, shelf, songs, mode, enabled, onTones, menu)
        SwitchingStrip(panel, enabled, dim)
        if (panel.idle) LinkCard(panel, discovery, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
    }
}

/** With no link there is nothing to turn: the controls dim to 38%, and the link card keeps full contrast. */
fun Modifier.dimmedUnless(linked: Boolean): Modifier = alpha(if (linked) 1f else DIMMED)

private const val DIMMED = 0.38f
