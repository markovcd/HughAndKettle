package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PanelStateTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val panel = PanelState(AmpModels.blackSpirit200, holder, scope)
    private val gain = AmpModels.blackSpirit200.controls.first { it.id == "gain" }

    @After
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
    }

    private fun nextSeen(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    /** Waits, up to 5 s, for [holds] to come true of the panel. */
    private fun until(
        what: String,
        holds: (PanelState) -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!holds(panel)) {
            if (started.elapsedNow() > 5.seconds) fail("the panel never got to: $what (link is ${panel.link})")
            Thread.sleep(5)
        }
    }

    private fun connected() {
        panel.host = "127.0.0.1"
        panel.port = "${fake.port}"
        panel.connect()
        until("connected") { it.link == LinkState.Connected(WmiAddress("127.0.0.1", fake.port)) }
        assertEquals(FakeWmiEvent.Opened::class, nextSeen()::class)
    }

    @Test
    fun `A control moved on a connected panel reaches the WMI-1`() {
        connected()
        panel.set(gain, 100)
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), gain.controller, 100)), nextSeen())
    }

    @Test
    fun `A knob turned on the amp moves on the panel`() {
        connected()
        fake.emit(ControlChange(MidiChannel(1), gain.controller, 42))
        until("gain at 42") { it.values["gain"] == 42 }
    }

    @Test
    fun `A box that drops is shown retrying, and disconnecting goes idle`() {
        connected()
        fake.close()
        until("retrying") { it.link is LinkState.Retrying }
        panel.disconnect()
        until("idle") { it.idle }
    }

    @Test
    fun `A port that is not a number asks for an address and starts nothing`() {
        panel.host = "127.0.0.1"
        panel.port = "twenty"
        panel.connect()
        assertEquals(PanelFailure.NeedsAddress, panel.failure)
        assertEquals(LinkState.Idle, panel.link)
    }
}
