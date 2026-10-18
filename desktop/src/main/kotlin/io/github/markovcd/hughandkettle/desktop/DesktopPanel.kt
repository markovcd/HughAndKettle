package io.github.markovcd.hughandkettle.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.AmpPanel
import io.github.markovcd.hughandkettle.app.DiscoveryState
import io.github.markovcd.hughandkettle.app.PanelState
import io.github.markovcd.hughandkettle.app.PresetNamesState
import io.github.markovcd.hughandkettle.app.SongState
import io.github.markovcd.hughandkettle.app.ThemeChoice
import io.github.markovcd.hughandkettle.app.ToneShelf
import io.github.markovcd.hughandkettle.app.theme.HughAndKettleTheme
import kotlinx.coroutines.launch

/** The window's content: the phone's panel over the desktop's link, in the theme picked. */
@Composable
fun DesktopPanel(app: DesktopApp) {
    val scope = rememberCoroutineScope()
    val panel =
        remember {
            PanelState(
                AmpModels.all.first(),
                app.link,
                scope,
                app.home.lastWmi.last,
                app.home.amps.choices,
                app.home.amps::remember,
            )
        }
    val discovery = remember { DiscoveryState() }
    val presetNames = remember { PresetNamesState(app.home.presetNames, panel, scope) }
    val songs = remember { SongState(app.home.songs, panel, scope, click = app.click::play) }
    val shelf =
        remember {
            ToneShelf(app.home.tones, panel, scope, heard = app.link.heard, names = { presetNames.names })
        }
    val theme by app.home.theme.choice
        .collectAsState(initial = ThemeChoice.Dark)
    HughAndKettleTheme(dark = theme == ThemeChoice.Dark || (theme == ThemeChoice.System && isSystemInDarkTheme())) {
        Surface(Modifier.fillMaxSize()) {
            AmpPanel(
                panel,
                discovery,
                shelf,
                presetNames,
                songs,
                theme,
                onTheme = { scope.launch { app.home.theme.pick(it) } },
            )
        }
    }
}
