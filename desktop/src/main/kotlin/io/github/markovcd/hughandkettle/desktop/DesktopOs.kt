package io.github.markovcd.hughandkettle.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut

/** What differs on the operating system the window runs on. */
internal object DesktopOs {
    val mac: Boolean = System.getProperty("os.name").orEmpty().startsWith("Mac")

    /** A menu item's shortcut on [key]: with Command on a Mac, with Control elsewhere. */
    fun shortcut(
        key: Key,
        mac: Boolean = this.mac,
    ): KeyShortcut = KeyShortcut(key, ctrl = !mac, meta = mac)

    /** On a Mac, puts the menus in the screen's menu bar under the app's name; it has to run before AWT starts. */
    fun settle() {
        if (!mac) return
        System.setProperty("apple.laf.useScreenMenuBar", "true")
        System.setProperty("apple.awt.application.name", "Hugh & Kettle")
    }
}
