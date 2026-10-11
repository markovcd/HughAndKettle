package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.wmi.LinkState.Connected
import io.github.markovcd.hughandkettle.wmi.LinkState.Connecting
import io.github.markovcd.hughandkettle.wmi.LinkState.Failure
import io.github.markovcd.hughandkettle.wmi.LinkState.Idle
import io.github.markovcd.hughandkettle.wmi.LinkState.Retrying
import java.io.IOException
import java.net.ServerSocket
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class LinkStateTest {
    private val box = Target("192.168.1.50", 2000)
    private val other = Target("192.168.1.51", 2000)

    private val oneOfEach: List<LinkState> =
        listOf(
            Idle,
            Connecting(box, failures = 2),
            Connected(box),
            Retrying(box, failures = 2, Failure.Refused, 2.seconds),
        )

    private val reports: Map<String, (LinkState) -> LinkState> =
        mapOf(
            "connect" to { it.connect(other) },
            "opened" to { it.opened() },
            "failed" to { it.failed(Failure.Unreachable("no route")) },
            "lost" to { it.lost("reset") },
            "retry" to { it.retry() },
        )

    @Test
    fun `Asking for a link starts the first attempt`() {
        assertEquals(Connecting(box, failures = 0), Idle.connect(box))
    }

    @Test
    fun `An attempt that opens is a link to its target`() {
        assertEquals(Connected(box), Connecting(box, failures = 3).opened())
    }

    @Test
    fun `A failed attempt says why and waits before the next`() {
        assertEquals(
            Retrying(box, failures = 1, Failure.Refused, 1.seconds),
            Connecting(box, failures = 0).failed(Failure.Refused),
        )
    }

    @Test
    fun `A retry is the next attempt and keeps the count of failures`() {
        assertEquals(Connecting(box, failures = 2), Retrying(box, failures = 2, Failure.Refused, 2.seconds).retry())
    }

    @Test
    fun `Each failure in a row doubles the wait, up to ten seconds`() {
        val unreachable = Failure.Unreachable("no route")
        val waits =
            generateSequence(Idle.connect(box).failed(unreachable)) { it.retry().failed(unreachable) }
                .take(7)
                .map { (it as Retrying).wait }
                .toList()
        assertEquals(listOf(1, 2, 4, 8, 10, 10, 10).map { it.seconds }, waits)
    }

    @Test
    fun `A lost link is retried at once, with the count of failures started over`() {
        assertEquals(
            Retrying(box, failures = 0, Failure.Lost("reset"), Duration.ZERO),
            Connected(box).lost("reset"),
        )
    }

    @Test
    fun `Cancelling leaves no link wanted, from every state`() {
        val notIdle = oneOfEach.map { it.cancel() }.filter { it != Idle }
        assertEquals(emptyList(), notIdle)
    }

    @Test
    fun `A report that does not apply to the state changes nothing`() {
        // The one report each state acts on; every other pairing is a late or stray report.
        val applies =
            setOf(
                Idle::class to "connect",
                Connecting::class to "opened",
                Connecting::class to "failed",
                Connected::class to "lost",
                Retrying::class to "retry",
            )
        val changed =
            oneOfEach.flatMap { state ->
                reports
                    .filterKeys { (state::class to it) !in applies }
                    .mapNotNull { (name, report) ->
                        val next = report(state)
                        if (next != state) "$name on $state gave $next" else null
                    }
            }
        assertEquals(emptyList(), changed.take(5), "${changed.size} reports changed a state they do not apply to")
    }

    @Test
    fun `A box that turns the link away is told apart from one that cannot be reached`() {
        val closedPort = ServerSocket(0).use { it.localPort }
        val refused = WmiLink.open("127.0.0.1", closedPort).exceptionOrNull()!!
        assertEquals(Failure.Refused, Failure.of(refused))
        assertEquals(Failure.Unreachable("no route to host"), Failure.of(IOException("no route to host")))
    }
}
