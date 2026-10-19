package io.github.markovcd.hughandkettle.cli

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/** How long a command that keeps running runs: `--seconds` if given, else until stdin closes. */
internal object RunLength {
    /** Waits for [ended], [seconds] if given, or else stdin closing. */
    fun await(
        ended: CountDownLatch,
        seconds: Int?,
    ) {
        if (seconds != null) {
            ended.await(seconds.toLong(), TimeUnit.SECONDS)
            return
        }
        thread(name = "listen-stdin", isDaemon = true) {
            while (System.`in`.read() >= 0) {
                // Input is ignored; closing it is the signal to stop.
            }
            ended.countDown()
        }
        ended.await()
    }
}
