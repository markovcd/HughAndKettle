package io.github.markovcd.hughandkettle.app

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTimestamp
import android.media.AudioTrack
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

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

    /**
     * How long after it is asked for a click is heard: the click is played silently and timed by the moment the
     * sound output says its first frame went out, which also wakes the output. Zero when the output never says.
     */
    suspend fun lag(): Duration {
        track.setVolume(0f)
        track.stop()
        track.reloadStaticData()
        // Looped, so the output keeps playing long enough to report when its first frame went out.
        track.setLoopPoints(0, ClickSamples.samples.size, -1)
        val asked = System.nanoTime()
        track.play()
        val stamp = AudioTimestamp()
        var heard: Long? = null
        while (heard == null && (System.nanoTime() - asked).nanoseconds < LAG_LIMIT) {
            if (track.getTimestamp(stamp) && stamp.framePosition > 0) {
                heard = stamp.nanoTime - stamp.framePosition * 1_000_000_000L / ClickSamples.RATE
            } else {
                delay(POLL)
            }
        }
        track.stop()
        track.setLoopPoints(0, 0, 0)
        track.setVolume(1f)
        return heard?.let { (it - asked).nanoseconds } ?: Duration.ZERO
    }

    /** Sounds one click, cutting off one still sounding. */
    fun play() {
        track.stop()
        track.reloadStaticData()
        track.play()
    }

    private companion object {
        val LAG_LIMIT = 1.seconds
        val POLL = 5.milliseconds
    }
}
