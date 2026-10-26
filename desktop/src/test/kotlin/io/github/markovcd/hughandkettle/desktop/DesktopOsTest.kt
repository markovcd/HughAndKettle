package io.github.markovcd.hughandkettle.desktop

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import kotlin.test.Test
import kotlin.test.assertEquals

class DesktopOsTest {
    @Test
    fun `A menu shortcut takes Command on a Mac and Control elsewhere`() {
        assertEquals(KeyShortcut(Key.T, meta = true), DesktopOs.shortcut(Key.T, mac = true))
        assertEquals(KeyShortcut(Key.T, ctrl = true), DesktopOs.shortcut(Key.T, mac = false))
    }
}
