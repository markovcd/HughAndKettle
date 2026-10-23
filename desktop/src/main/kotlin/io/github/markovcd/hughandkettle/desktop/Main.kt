package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import io.github.markovcd.hughandkettle.app.SwitcherMode
import io.github.markovcd.hughandkettle.app.ThemeChoice
import kotlinx.coroutines.launch
import java.awt.Dimension
import javax.imageio.ImageIO

/** The desktop app: a window over the shared screens, keeping its things in `~/.hk` beside `hk`'s. */
fun main() {
    val app = DesktopApp(DesktopApp.HK_FOLDER)
    // The same picture the .deb and the MSIX show, for the title bar and the task bar.
    val icon = BitmapPainter(ImageIO.read(DesktopApp::class.java.getResource("/icon.png")).toComposeImageBitmap())
    application {
        // Four panes side by side want about 300 dp each; narrower, the amp's knobs no longer fit four to a row.
        val window = rememberWindowState(width = 1440.dp, height = 900.dp)
        Window(onCloseRequest = ::exitApplication, state = window, title = "Hugh & Kettle", icon = icon) {
            this.window.minimumSize = Dimension(1200, 720)
            val scope = rememberCoroutineScope()
            val screen = rememberDesktopScreen(app)
            val theme by app.home.theme.choice
                .collectAsState(initial = ThemeChoice.Dark)
            val mode by app.home.switcher.mode
                .collectAsState(initial = SwitcherMode.Presets)
            DesktopMenuBar(
                screen,
                theme,
                onTheme = { scope.launch { app.home.theme.pick(it) } },
                mode,
                onMode = { scope.launch { app.home.switcher.pick(it) } },
            )
            DesktopPanel(screen, theme, mode)
        }
    }
    app.close()
}
