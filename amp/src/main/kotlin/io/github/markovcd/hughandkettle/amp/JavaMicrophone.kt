package io.github.markovcd.hughandkettle.amp

import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

/** The machine's default microphone, through Java's own sound, for `hk` and the desktop app. */
public object JavaMicrophone : Microphone {
    override fun open(): Result<MicrophoneInput> =
        runCatching {
            val format = AudioFormat(RATE, 16, 1, true, false)
            val line = AudioSystem.getTargetDataLine(format)
            line.open(format)
            line.start()
            Input(line)
        }.recoverCatching { throw IllegalStateException("cannot open the microphone: ${it.message}", it) }

    private class Input(
        private val line: TargetDataLine,
    ) : MicrophoneInput {
        private val bytes = ByteArray(FRAME * 2)
        private val samples = ShortArray(FRAME)

        override fun read(): Result<LevelFrame> {
            var read = 0
            while (read < bytes.size) {
                val got = line.read(bytes, read, bytes.size - read)
                if (got <= 0) return Result.failure(IllegalStateException("the microphone stopped"))
                read += got
            }
            for (i in samples.indices) {
                samples[i] = ((bytes[2 * i + 1].toInt() shl 8) or (bytes[2 * i].toInt() and 0xFF)).toShort()
            }
            return Result.success(LevelFrame.of(samples))
        }

        override fun close() {
            line.stop()
            line.close()
        }
    }

    private const val RATE = 44_100f

    // Samples in one frame of LevelFrame.LENGTH at RATE.
    private const val FRAME = 2_205
}
