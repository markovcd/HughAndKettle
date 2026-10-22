package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.app.ClickSamples
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * The metronome click through Java's sound, loaded when it is made so the first click of a count-in sounds on
 * its beat. A machine with no sound output plays nothing.
 */
class DesktopClickSound {
    private val clip: Clip? =
        runCatching {
            val bytes = ByteArray(ClickSamples.samples.size * 2)
            ClickSamples.samples.forEachIndexed { i, sample ->
                bytes[2 * i] = sample.toByte()
                bytes[2 * i + 1] = (sample.toInt() shr 8).toByte()
            }
            val format = AudioFormat(ClickSamples.RATE.toFloat(), 16, 1, true, false)
            AudioSystem.getClip().apply { open(format, bytes, 0, bytes.size) }
        }.getOrNull()

    /** Sounds one click, cutting off one still sounding. */
    fun play() {
        val clip = clip ?: return
        clip.stop()
        clip.framePosition = 0
        clip.start()
    }
}
