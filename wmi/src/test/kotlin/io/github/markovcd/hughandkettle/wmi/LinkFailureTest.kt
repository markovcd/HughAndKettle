package io.github.markovcd.hughandkettle.wmi

import java.net.ConnectException
import java.net.SocketTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals

class LinkFailureTest {
    @Test
    fun `A box that never answers is unreachable in plain words`() {
        val timedOut =
            SocketTimeoutException("failed to connect to /192.168.1.1 (port 1470) from /192.168.1.10 after 5000ms")
        assertEquals(LinkFailure.Unreachable("no answer within 5s"), LinkFailure.of(timedOut))
    }

    @Test
    fun `A box that says no is refused`() {
        assertEquals(LinkFailure.Refused, LinkFailure.of(ConnectException("Connection refused")))
    }
}
