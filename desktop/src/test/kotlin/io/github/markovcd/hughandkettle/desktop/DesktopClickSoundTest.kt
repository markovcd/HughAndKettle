package io.github.markovcd.hughandkettle.desktop

import kotlin.test.Test

class DesktopClickSoundTest {
    @Test
    fun `Clicking twice in a row never fails, with or without sound output`() {
        val click = DesktopClickSound()
        click.play()
        click.play()
    }
}
