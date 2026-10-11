package io.github.markovcd.hughandkettle.wmi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class BackoffTest {
    @Test
    fun `The wait doubles from the first and stops at the ceiling`() {
        val backoff = Backoff(1.seconds, 30.seconds)
        assertEquals(
            listOf(1, 2, 4, 8, 16, 30, 30).map { it.seconds },
            (1..7).map(backoff::after),
        )
    }

    @Test
    fun `A thousand failures in a row wait the ceiling and do not overflow`() {
        assertEquals(30.seconds, Backoff.Default.after(1000))
    }
}
