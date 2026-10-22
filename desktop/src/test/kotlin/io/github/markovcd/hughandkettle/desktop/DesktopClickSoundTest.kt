package io.github.markovcd.hughandkettle.desktop

import org.junit.jupiter.api.Assumptions.assumeTrue
import javax.sound.sampled.AudioSystem
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTime

class DesktopClickSoundTest {
    @Test
    fun `Clicking twice in a row never fails, with or without sound output`() {
        val click = DesktopClickSound()
        click.play()
        click.play()
    }

    @Test
    fun `The first click starts at once, its sound loaded when the click sound was made`() {
        assumeTrue(runCatching { AudioSystem.getClip().close() }.isSuccess, "skip when no sound output on this machine")
        val click = DesktopClickSound()
        val took = measureTime { click.play() }
        // Opening a clip takes tens of milliseconds; starting a loaded one takes well under one.
        assertTrue(took < 20.milliseconds, "the first click took $took to start")
    }
}
