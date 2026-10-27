package io.github.markovcd.hughandkettle.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.markovcd.hughandkettle.amp.LevelFrame
import io.github.markovcd.hughandkettle.amp.Microphone
import io.github.markovcd.hughandkettle.amp.MicrophoneInput

/**
 * The phone's microphone, with no gain control or noise suppression where the phone allows: unprocessed if it
 * offers that, else the voice recognition source, which Android keeps free of automatic gain.
 */
internal class PhoneMicrophone(
    private val context: Context,
) : Microphone {
    override fun open(): Result<MicrophoneInput> {
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return Result.failure(IllegalStateException("the microphone is not allowed"))
        }
        val audio = context.getSystemService(AudioManager::class.java)
        val unprocessed = audio?.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED) == "true"
        val source =
            if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
        val smallest = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        if (smallest <= 0) return Result.failure(IllegalStateException("the microphone cannot record at $RATE Hz"))
        return try {
            val record =
                AudioRecord(
                    source,
                    RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    maxOf(smallest, FRAME * 2 * FRAMES_BUFFERED),
                )
            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                return Result.failure(IllegalStateException("the microphone is in use or missing"))
            }
            record.startRecording()
            Result.success(Input(record))
        } catch (e: SecurityException) {
            Result.failure(IllegalStateException("the microphone is not allowed", e))
        }
    }

    private class Input(
        private val record: AudioRecord,
    ) : MicrophoneInput {
        private val samples = ShortArray(FRAME)

        override fun read(): Result<LevelFrame> {
            var read = 0
            while (read < samples.size) {
                val got = record.read(samples, read, samples.size - read)
                if (got <= 0) return Result.failure(IllegalStateException("the microphone stopped ($got)"))
                read += got
            }
            return Result.success(LevelFrame.of(samples))
        }

        override fun close() {
            record.stop()
            record.release()
        }
    }

    private companion object {
        const val RATE = 44_100

        // Samples in one frame of LevelFrame.LENGTH at RATE, and frames the recorder holds before they are read.
        const val FRAME = 2_205
        const val FRAMES_BUFFERED = 4
    }
}
