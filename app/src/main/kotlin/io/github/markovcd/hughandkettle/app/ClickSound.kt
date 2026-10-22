package io.github.markovcd.hughandkettle.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

/**
 * The metronome click, loaded when it is made so the first click of a count-in sounds as soon as it is asked
 * for, on its beat, rather than after the track is built.
 */
class ClickSound {
    private val track: AudioTrack =
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
            .setBufferSizeInBytes(ClickSamples.samples.size * 2)
            .build()
            .also { it.write(ClickSamples.samples, 0, ClickSamples.samples.size) }

    /** Sounds one click, cutting off one still sounding. */
    fun play() {
        track.stop()
        track.reloadStaticData()
        track.play()
    }
}
