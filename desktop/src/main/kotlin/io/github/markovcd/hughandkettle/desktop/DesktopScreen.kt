package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.DiscoveryState
import io.github.markovcd.hughandkettle.app.PanelState
import io.github.markovcd.hughandkettle.app.PresetNamesState
import io.github.markovcd.hughandkettle.app.SongState
import io.github.markovcd.hughandkettle.app.ToneShelf
import kotlinx.coroutines.CoroutineScope

/** What the window shows and its menu bar acts on, over [app]'s link and stores. */
class DesktopScreen(
    app: DesktopApp,
    scope: CoroutineScope,
) {
    val panel: PanelState =
        PanelState(
            AmpModels.all.first(),
            app.link,
            scope,
            app.home.lastWmi.last,
            app.home.amps.choices,
            app.home.amps::remember,
        )
    val discovery: DiscoveryState = DiscoveryState()
    val presetNames: PresetNamesState = PresetNamesState(app.home.presetNames, panel, scope)
    val songs: SongState = SongState(app.home.songs, panel, scope, click = app.click::play)
    val shelf: ToneShelf =
        ToneShelf(app.home.tones, panel, scope, heard = app.link.heard, names = { presetNames.names })
    val commands: DesktopCommands = DesktopCommands(panel, shelf, songs)
}

@Composable
fun rememberDesktopScreen(app: DesktopApp): DesktopScreen {
    val scope = rememberCoroutineScope()
    return remember { DesktopScreen(app, scope) }
}
