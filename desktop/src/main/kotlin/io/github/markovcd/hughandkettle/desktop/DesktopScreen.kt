package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import io.github.markovcd.hughandkettle.app.DiscoveryState
import io.github.markovcd.hughandkettle.app.PanelScreen
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
    private val screen = PanelScreen(app.home, app.link, scope, DiscoveryState(), click = app.click::play)
    val panel: PanelState = screen.panel
    val discovery: DiscoveryState = screen.discovery
    val presetNames: PresetNamesState = screen.presetNames
    val songs: SongState = screen.songs
    val shelf: ToneShelf = screen.shelf
    val commands: DesktopCommands = DesktopCommands(panel, shelf, songs)
}

@Composable
fun rememberDesktopScreen(app: DesktopApp): DesktopScreen {
    val scope = rememberCoroutineScope()
    return remember { DesktopScreen(app, scope) }
}
