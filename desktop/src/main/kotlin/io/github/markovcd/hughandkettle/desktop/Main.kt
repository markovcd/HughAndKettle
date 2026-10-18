package io.github.markovcd.hughandkettle.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/** The desktop app: a window over the shared screens, keeping its things in `~/.hk` beside `hk`'s. */
fun main() {
    val app = DesktopApp(DesktopApp.HK_FOLDER)
    application {
        // Wide enough for the amp and its effects side by side, as on a phone in landscape.
        val window = rememberWindowState(width = 1100.dp, height = 720.dp)
        Window(onCloseRequest = ::exitApplication, state = window, title = "Hugh & Kettle") {
            DesktopPanel(app)
        }
    }
    app.close()
}
