package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.wmi.LinkState.Connected
import io.github.markovcd.hughandkettle.wmi.LinkState.Connecting
import io.github.markovcd.hughandkettle.wmi.LinkState.Idle
import io.github.markovcd.hughandkettle.wmi.LinkState.Retrying
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LinkStateTest {
    private val box = WmiAddress("192.168.1.20", 2000)
    private val other = WmiAddress("192.168.1.21", 2000)
    private val backoff = Backoff(1.seconds, 30.seconds)
    private val lost = LinkFailure.Lost("closed by the WMI-1")

    private fun LinkState.after(
        event: LinkEvent,
        now: Duration = 10.seconds,
    ): LinkState = on(event, now, backoff)

    @Test
    fun `Asking for a box starts the first attempt`() {
        assertEquals(Connecting(box, 1), Idle.after(LinkEvent.Connect(box)))
    }

    @Test
    fun `An attempt that opens is connected`() {
        assertEquals(Connected(box), Connecting(box, 3).after(LinkEvent.Opened))
    }

    @Test
    fun `An attempt that fails waits the backoff for that many failures`() {
        assertEquals(
            Retrying(box, 3, LinkFailure.Refused, 14.seconds),
            Connecting(box, 3).after(LinkEvent.Failed(LinkFailure.Refused), now = 10.seconds),
        )
    }

    @Test
    fun `A link that drops waits the first backoff, whatever came before`() {
        assertEquals(Retrying(box, 1, lost, 11.seconds), Connected(box).after(LinkEvent.Failed(lost), now = 10.seconds))
    }

    @Test
    fun `A retry starts when it is due and not before`() {
        val waiting = Retrying(box, 2, lost, 12.seconds)
        assertEquals(waiting, waiting.after(LinkEvent.Tick, now = 11.seconds))
        assertEquals(Connecting(box, 3), waiting.after(LinkEvent.Tick, now = 12.seconds))
    }

    @Test
    fun `Stopping goes back to idle from every state`() {
        listOf(Idle, Connecting(box, 1), Connected(box), Retrying(box, 1, lost, 11.seconds)).forEach {
            assertEquals(Idle, it.after(LinkEvent.Stop), "from $it")
        }
    }

    @Test
    fun `Asking for another box starts over on it from every state`() {
        listOf(Idle, Connecting(box, 4), Connected(box), Retrying(box, 4, lost, 11.seconds)).forEach {
            assertEquals(Connecting(other, 1), it.after(LinkEvent.Connect(other)), "from $it")
        }
    }

    @Test
    fun `News that belongs to no attempt changes nothing`() {
        val cases =
            listOf(
                Idle to LinkEvent.Opened,
                Idle to LinkEvent.Failed(lost),
                Idle to LinkEvent.Tick,
                Connected(box) to LinkEvent.Opened,
                Connected(box) to LinkEvent.Tick,
                Connecting(box, 1) to LinkEvent.Tick,
                Retrying(box, 1, lost, 11.seconds) to LinkEvent.Opened,
                Retrying(box, 1, lost, 11.seconds) to LinkEvent.Failed(lost),
            )
        val moved = cases.filter { (state, event) -> state.after(event, now = 0.seconds) != state }
        assertEquals(emptyList(), moved)
    }
}
