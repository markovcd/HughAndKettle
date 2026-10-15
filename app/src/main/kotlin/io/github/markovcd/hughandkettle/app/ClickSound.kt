package io.github.markovcd.hughandkettle.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/**
 * A metronome click, made once and held loaded, so a click sounds as soon as it is asked for rather than
 * after a sound is decoded: a short tone that dies away fast enough to read as a tick.
 */
class ClickSound {
    private val track: AudioTrack by lazy {
        val samples =
            ShortArray(RATE * LENGTH_MS / 1000) { i ->
                val t = i.toDouble() / RATE
                (Short.MAX_VALUE * LOUDNESS * exp(-t * DECAY) * sin(2 * PI * PITCH * t)).toInt().toShort()
            }
        AudioTrack
            .Builder()
            .setAudioAttributes(
                AudioAttributes
                    .Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            ).setAudioFormat(
                AudioFormat
                    .Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            ).setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
            .also { it.write(samples, 0, samples.size) }
    }

    /** Sounds one click, cutting off one still sounding. */
    fun play() {
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    private companion object {
        const val RATE = 48_000
        const val LENGTH_MS = 40
        const val PITCH = 1_500.0

        // Down to a twentieth by 20 ms: a tick, not a beep.
        const val DECAY = 150.0
        const val LOUDNESS = 0.8
    }
}
