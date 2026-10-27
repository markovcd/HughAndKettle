package io.github.markovcd.hughandkettle.amp

import kotlin.math.PI
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class LevelFrameTest {
    @Test
    fun `A full scale sine is 3 dB below full scale and peaks at the top`() {
        val sine = ShortArray(2205) { (32767 * sin(2 * PI * 441 * it / 44100.0)).toInt().toShort() }
        val frame = LevelFrame.of(sine)
        assertEquals(-3.01, frame.level, 0.05)
        assertEquals(1.0, frame.peak, 0.001)
    }

    @Test
    fun `Silence is the floor of the scale`() {
        assertEquals(LevelFrame(LevelFrame.SILENCE, 0.0), LevelFrame.of(ShortArray(100)))
    }

    @Test
    fun `Only the samples counted are heard`() {
        val samples = ShortArray(100) { if (it < 50) 0 else Short.MAX_VALUE }
        assertEquals(LevelFrame.SILENCE, LevelFrame.of(samples, 50).level)
    }
}
