package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.markovcd.hughandkettle.app.ThemeChoice
import kotlinx.coroutines.launch
import java.awt.Dimension

/** The desktop app: a window over the shared screens, keeping its things in `~/.hk` beside `hk`'s. */
fun main() {
    val app = DesktopApp(DesktopApp.HK_FOLDER)
    application {
        // Four panes side by side want about 300 dp each; narrower, the amp's knobs no longer fit four to a row.
        val window = rememberWindowState(width = 1440.dp, height = 900.dp)
        Window(onCloseRequest = ::exitApplication, state = window, title = "Hugh & Kettle") {
            this.window.minimumSize = Dimension(1200, 720)
            val scope = rememberCoroutineScope()
            val screen = rememberDesktopScreen(app)
            val theme by app.home.theme.choice
                .collectAsState(initial = ThemeChoice.Dark)
            DesktopMenuBar(screen, theme, onTheme = { scope.launch { app.home.theme.pick(it) } })
            DesktopPanel(screen, theme)
        }
    }
    app.close()
}
