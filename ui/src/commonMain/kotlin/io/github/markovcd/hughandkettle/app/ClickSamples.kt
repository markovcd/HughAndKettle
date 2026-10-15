package io.github.markovcd.hughandkettle.app

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** The metronome click both apps play: a short tone that dies away fast enough to read as a tick. */
object ClickSamples {
    const val RATE = 48_000

    /** The click as 16-bit mono samples at [RATE]. */
    val samples: ShortArray by lazy {
        ShortArray(RATE * LENGTH_MS / 1000) { i ->
            val t = i.toDouble() / RATE
            (Short.MAX_VALUE * LOUDNESS * exp(-t * DECAY) * sin(2 * PI * PITCH * t)).toInt().toShort()
        }
    }

    private const val LENGTH_MS = 40
    private const val PITCH = 1_500.0

    // Down to a twentieth by 20 ms: a tick, not a beep.
    private const val DECAY = 150.0
    private const val LOUDNESS = 0.8
}
