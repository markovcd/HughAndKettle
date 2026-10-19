package io.github.markovcd.hughandkettle.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.markovcd.hughandkettle.app.ThemeChoice
import io.github.markovcd.hughandkettle.app.theme.HughAndKettleTheme

/** The window's content: the desktop's layout of [screen], in [theme]. */
@Composable
fun DesktopPanel(
    screen: DesktopScreen,
    theme: ThemeChoice,
) {
    HughAndKettleTheme(dark = theme == ThemeChoice.Dark || (theme == ThemeChoice.System && isSystemInDarkTheme())) {
        Surface(Modifier.fillMaxSize()) {
            DesktopLayout(screen.panel, screen.discovery, screen.shelf, screen.presetNames, screen.songs)
        }
    }
}
