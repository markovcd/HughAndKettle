package io.github.markovcd.hughandkettle.desktop

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.app.AmpQuestion
import io.github.markovcd.hughandkettle.app.AmpTab
import io.github.markovcd.hughandkettle.app.DiscoveryState
import io.github.markovcd.hughandkettle.app.FxTab
import io.github.markovcd.hughandkettle.app.LocalHints
import io.github.markovcd.hughandkettle.app.PanelState
import io.github.markovcd.hughandkettle.app.PanelTop
import io.github.markovcd.hughandkettle.app.PresetNamesState
import io.github.markovcd.hughandkettle.app.SongState
import io.github.markovcd.hughandkettle.app.SongTab
import io.github.markovcd.hughandkettle.app.SwitcherMode
import io.github.markovcd.hughandkettle.app.ToneShelf
import io.github.markovcd.hughandkettle.app.ToneShelfCard
import io.github.markovcd.hughandkettle.app.dimmedUnless

/**
 * The desktop's panel: no tabs, but the amp, its effects, the song and the tones side by side, each
 * scrolling on its own, under the panel's top, whose − and + step as [mode] says. The settings are in the menu bar, and controls say what they
 * do while the pointer rests on them. Tests find the panes by `amp`, `fx`, `song` and `tones`.
 */
@Composable
fun DesktopLayout(
    panel: PanelState,
    discovery: DiscoveryState,
    shelf: ToneShelf,
    presetNames: PresetNamesState,
    songs: SongState,
    mode: SwitcherMode,
) {
    val enabled = panel.linked
    val dim = Modifier.dimmedUnless(enabled)
    if (panel.asking) AmpQuestion(panel)
    CompositionLocalProvider(LocalHints provides true) {
        Column(Modifier.fillMaxSize()) {
            // The tones pane is always in view, so the header has no tab to go to, and its menu is the menu bar.
            PanelTop(panel, discovery, presetNames, shelf, songs, mode, onTones = {}, menu = null)
            Row(Modifier.weight(1f)) {
                AmpTab(
                    panel,
                    songs,
                    enabled,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(dim)
                        .testTag("amp"),
                )
                VerticalDivider()
                FxTab(
                    panel,
                    songs,
                    enabled,
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .then(dim)
                        .testTag("fx"),
                )
                VerticalDivider()
                SongTab(
                    songs,
                    presetNames,
                    panel.state.preset,
                    enabled,
                    Modifier.weight(1f).testTag("song"),
                    mode,
                    shelf.loaded,
                )
                VerticalDivider()
                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(12.dp)
                        .testTag("tones"),
                ) {
                    ToneShelfCard(shelf, enabled)
                }
            }
        }
    }
}
