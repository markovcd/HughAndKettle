package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import java.net.ServerSocket
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LinkKeeperTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val states = LinkedBlockingQueue<LinkState>()
    private val heard = LinkedBlockingQueue<MidiMessage>()
    private val fakes = mutableListOf<FakeWmi>()

    // Short enough that a test of a retry takes a fraction of a second.
    private val keeper = LinkKeeper(states::put, heard::put, Backoff(50.milliseconds, 200.milliseconds))

    @AfterTest
    fun teardown() {
        keeper.close()
        fakes.forEach(FakeWmi::close)
    }

    private fun fake(port: Int = 0): FakeWmi = FakeWmi(port, onEvent = seen::put).also { fakes += it }

    private fun nextState(): LinkState = assertNotNull(states.poll(5, TimeUnit.SECONDS), "no state within 5 s")

    private fun nextSeen(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    private fun address(fake: FakeWmi) = WmiAddress("127.0.0.1", fake.port)

    @Test
    fun `A box that answers is connected, and messages go both ways`() {
        val fake = fake()
        keeper.connect(address(fake))
        assertEquals(LinkState.Connecting(address(fake), 1), nextState())
        assertEquals(LinkState.Connected(address(fake)), nextState())
        assertIs<FakeWmiEvent.Opened>(nextSeen())

        keeper.send(ControlChange(MidiChannel(1), 20, 100)).getOrThrow()
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), 20, 100)), nextSeen())
        assertTrue(fake.emit(ProgramChange(MidiChannel(1), 3)))
        assertEquals(ProgramChange(MidiChannel(1), 3), heard.poll(5, TimeUnit.SECONDS))
    }

    @Test
    fun `A box that comes back after a power cycle is reconnected on its own`() {
        val first = fake()
        val address = address(first)
        keeper.connect(address)
        assertEquals(
            LinkState.Connected(address),
            generateSequence { nextState() }.first { it !is LinkState.Connecting },
        )
        first.close()

        val retrying = assertIs<LinkState.Retrying>(nextState())
        assertIs<LinkFailure.Lost>(retrying.failure)
        // The box is back on the same port; the keeper finds it without being asked again.
        fake(address.port)
        val after = generateSequence { nextState() }.first { it is LinkState.Connected }
        assertEquals(LinkState.Connected(address), after)
    }

    @Test
    fun `A port nothing listens on is refused and retried with a growing count`() {
        val closedPort = ServerSocket(0).use { it.localPort }
        keeper.connect(WmiAddress("127.0.0.1", closedPort))
        val failures =
            generateSequence { nextState() }
                .filterIsInstance<LinkState.Retrying>()
                .take(3)
                .toList()
        assertEquals(listOf(1, 2, 3), failures.map { it.failures })
        assertTrue(failures.all { it.failure == LinkFailure.Refused }, "failures: $failures")
    }

    @Test
    fun `Stopping drops the link and stays idle`() {
        val fake = fake()
        keeper.connect(address(fake))
        generateSequence { nextState() }.first { it is LinkState.Connected }
        assertIs<FakeWmiEvent.Opened>(nextSeen())
        keeper.stop()
        assertEquals(LinkState.Idle, nextState())
        assertEquals(FakeWmiEvent.Closed, nextSeen())
        assertTrue(keeper.send(ProgramChange(MidiChannel(1), 1)).isFailure)
        // Several backoffs long: a retry the stop failed to cancel would have reported a state by now.
        assertEquals(null, states.poll(500, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `Closing ends the keeper's thread`() {
        keeper.connect(address(fake()))
        generateSequence { nextState() }.first { it is LinkState.Connected }
        keeper.close()
        assertTrue(keeper.awaitClosed(5.seconds))
        assertEquals(LinkState.Idle, keeper.state)
    }

    @Test
    fun `A box that hangs up the moment it answers was still connected before the link was lost`() {
        val box = ServerSocket(0)
        val hangingUp =
            thread(isDaemon = true) {
                runCatching { while (true) box.accept().close() }
            }
        val address = WmiAddress("127.0.0.1", box.localPort)
        // The hang-up races the keeper's own news of the open, so it is run enough times to lose the race.
        val wrong =
            (1..ROUNDS).mapNotNull { round ->
                keeper.connect(address)
                assertEquals(LinkState.Connecting(address, 1), nextState())
                val after = nextState()
                keeper.stop()
                generateSequence { nextState() }.first { it == LinkState.Idle }
                after.takeIf { it != LinkState.Connected(address) }?.let { "round $round: $it" }
            }
        box.close()
        hangingUp.join(5_000)
        assertEquals(emptyList(), wrong.take(3))
    }

    private companion object {
        const val ROUNDS = 300
    }
}
