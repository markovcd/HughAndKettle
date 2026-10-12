package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Test
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PanelStateTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val last = MutableStateFlow<WmiAddress?>(null)
    private val panel = PanelState(AmpModels.blackSpirit200, holder, scope, last)
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
        until("gain at 42") { it.state.values["gain"] == 42 }
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

    @Test
    fun `The last box fills empty fields and leaves typed ones alone`() {
        last.value = WmiAddress("192.168.1.20", 2001)
        assertEquals("192.168.1.20" to "2001", panel.host to panel.port)
        panel.host = "10.0.0.5"
        last.value = WmiAddress("192.168.1.21", 2000)
        assertEquals("10.0.0.5", panel.host)
        assertEquals(WmiAddress("192.168.1.21", 2000), panel.last)
    }

    @Test
    fun `A preset recalled on the panel reaches the WMI-1 as its program change and becomes the current preset`() {
        connected()
        panel.recall(6)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 5)), nextSeen())
        assertEquals(6, panel.state.preset)
    }

    @Test
    fun `A preset changed on the amp shows on the panel and forgets the values it had`() {
        connected()
        panel.set(gain, 100)
        nextSeen()
        fake.emit(ProgramChange(MidiChannel(1), 9))
        until("preset 10") { it.state.preset == 10 }
        assertEquals(emptyMap(), panel.state.values)
    }

    @Test
    fun `Tapping a beat on delay time moves it to the beat, from the second tap`() {
        val delay = AmpModels.blackSpirit200.control("delay_time")!!
        panel.tap(delay, 10.seconds)
        assertEquals(null, panel.state.values["delay_time"])
        panel.tap(delay, 10.seconds + 500.milliseconds)
        assertEquals(delay.scale!!.value(500), panel.state.values["delay_time"])
    }

    @Test
    fun `A picked amp starts from nothing known and is remembered for the last box`() {
        val remembered = ArrayList<Pair<WmiAddress, String>>()
        val home = WmiAddress("192.168.1.1", 2000)
        val picking =
            PanelState(AmpModels.blackSpirit200, holder, scope, MutableStateFlow(home), rememberAmp = { box, amp ->
                remembered += box to amp.id
            })
        picking.set(gain, 100)
        picking.choose(AmpModels.grandMeisterDeluxe40)
        assertEquals(AmpModels.grandMeisterDeluxe40, picking.amp)
        assertEquals(emptyMap(), picking.state.values)
        assertEquals(listOf(home to "grandmeister_deluxe_40"), remembered)
    }

    @Test
    fun `Linking to a box shows the amp picked for it`() {
        val box = WmiAddress("127.0.0.1", fake.port)
        val following =
            PanelState(
                AmpModels.blackSpirit200,
                holder,
                scope,
                last,
                MutableStateFlow(
                    mapOf(box to AmpModels.grandMeisterDeluxe40),
                ),
            )
        assertEquals(AmpModels.blackSpirit200, following.amp)
        following.connect(box)
        until("connected") { it.link == LinkState.Connected(box) }
        assertEquals(AmpModels.grandMeisterDeluxe40, following.amp)
    }
}
