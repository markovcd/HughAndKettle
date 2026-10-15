package io.github.markovcd.hughandkettle.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * The metronome click, held loaded so it sounds as soon as it is asked for rather than after a sound is
 * decoded.
 */
class ClickSound {
    private val track: AudioTrack by lazy {
        val samples = ClickSamples.samples
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
                    .setSampleRate(ClickSamples.RATE)
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
}
