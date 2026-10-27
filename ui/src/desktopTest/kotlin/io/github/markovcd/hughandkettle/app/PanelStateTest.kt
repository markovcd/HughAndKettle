package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LoudnessCorrections
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.amp.VolumeTrim
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
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
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class PanelStateTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val jack = PluggedJack()
    private val holder = LinkHolder(jack)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val last = MutableStateFlow<WmiAddress?>(null)
    private val panel = PanelState(AmpModels.blackSpirit200, holder, scope, last)
    private val gain = AmpModels.blackSpirit200.controls.first { it.id == "gain" }

    @AfterTest
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
    fun `Muting sends the amp's Mute switch, and muted follows the switch as set and as heard`() {
        connected()
        val mute = AmpModels.blackSpirit200.control("mute")!!
        assertEquals(false, panel.muted)

        panel.mute(true)

        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 127)), nextSeen())
        assertEquals(true, panel.muted)
        fake.emit(ControlChange(MidiChannel(1), mute.controller, 0))
        until("unmuted by the amp") { !it.muted }
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
    fun `A knob the amp moves is marked until the panel sets it`() {
        connected()
        fake.emit(ControlChange(MidiChannel(1), gain.controller, 42))
        until("gain marked") { "gain" in it.moved }
        panel.set(gain, 50)
        assertEquals(emptySet(), panel.moved)
    }

    @Test
    fun `The amp repeating a value the panel set marks nothing`() {
        connected()
        panel.set(gain, 42)
        nextSeen()
        fake.emit(ControlChange(MidiChannel(1), gain.controller, 42))
        fake.emit(ProgramChange(MidiChannel(1), 3))
        until("preset 4") { it.state.preset == 4 }
        assertEquals(emptySet(), panel.moved)
    }

    @Test
    fun `A preset changed on the amp clears the marks`() {
        connected()
        fake.emit(ControlChange(MidiChannel(1), gain.controller, 42))
        until("gain marked") { "gain" in it.moved }
        fake.emit(ProgramChange(MidiChannel(1), 3))
        until("marks cleared") { it.moved.isEmpty() }
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
    fun `The only box heard while idle is linked to without a tap`() {
        val box = WmiAddress("127.0.0.1", fake.port)
        panel.offered(listOf(box))
        until("connected") { it.link == LinkState.Connected(box) }
    }

    @Test
    fun `Of several boxes heard, the last one used is linked to and none is when it is not among them`() {
        val box = WmiAddress("127.0.0.1", fake.port)
        val other = WmiAddress("127.0.0.2", fake.port)
        last.value = WmiAddress("127.0.0.3", fake.port)
        panel.offered(listOf(other, box))
        assertEquals(LinkState.Idle, panel.link)
        last.value = box
        panel.offered(listOf(other, box))
        until("connected") { it.link == LinkState.Connected(box) }
    }

    @Test
    fun `After a disconnect a box heard waits for a tap`() {
        connected()
        panel.disconnect()
        until("idle") { it.link == LinkState.Idle }
        panel.offered(listOf(WmiAddress("127.0.0.1", fake.port)))
        assertEquals(LinkState.Idle, panel.link)
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
    fun `The address starts as the WMI-1's own access point`() {
        assertEquals("192.168.1.1" to "1470", panel.host to panel.port)
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
    fun `Stepping the preset down from none known wraps to the last, and up from the last to the first`() {
        panel.step(-1)
        assertEquals(128, panel.state.preset)
        panel.step(1)
        assertEquals(1, panel.state.preset)
        panel.step(1)
        assertEquals(2, panel.state.preset)
    }

    @Test
    fun `A song's tone step under a trim and a lock sends the tone trimmed and the song's delay time`() {
        val delay = AmpModels.blackSpirit200.control("delay_time")!!
        val volume = AmpModels.blackSpirit200.control("volume")!!
        connected()
        panel.set(delay, 40)
        nextSeen()
        panel.lock(delay, on = true)
        panel.trim(-20)
        panel.cue(listOf(volume.set(100), delay.set(60)))
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), volume.controller, 80)), nextSeen())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), delay.controller, 60)), nextSeen())
        assertEquals(60, panel.lock?.value)
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
    fun `A preset recalled from the footswitch while the tempo is locked gets the locked delay time back`() {
        val delay = AmpModels.blackSpirit200.control("delay_time")!!
        connected()
        panel.set(delay, 40)
        nextSeen()
        panel.lock(delay, on = true)
        fake.emit(ProgramChange(MidiChannel(1), 2))
        fake.emit(ValueBlock(mapOf(delay.controller to 141), preset = 3).message())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), delay.controller, 40)), nextSeen())
        assertEquals(40, panel.state.values["delay_time"])
        assertEquals(1, panel.restored)
    }

    @Test
    fun `A preset recalled from the panel under a trim goes behind the mute and comes back trimmed`() {
        val volume = AmpModels.blackSpirit200.control("volume")!!
        val mute = AmpModels.blackSpirit200.control("mute")!!
        connected()
        panel.trim(-20)
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 127)), nextSeen())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), nextSeen())
        fake.emit(ValueBlock(mapOf(volume.controller to 120), preset = 3).message())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), volume.controller, 40)), nextSeen())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 0)), nextSeen())
    }

    @Test
    fun `A preset recalled with a loudness correction goes behind the mute and comes back corrected`() {
        val amp = AmpModels.blackSpirit200
        val volume = amp.control("volume")!!
        val mute = amp.control("mute")!!
        connected()
        panel.correct(LoudnessCorrections(amp, mapOf(3 to -11)))
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 127)), nextSeen())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), nextSeen())
        fake.emit(ValueBlock(mapOf(volume.controller to 120), preset = 3).message())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), volume.controller, 49)), nextSeen())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 0)), nextSeen())
        until("the correction counted") { it.corrected == 1 }
        assertEquals(VolumeTrim.Shift(stored = 60, correction = -11), panel.trim?.shift)
        assertEquals(0, panel.trimmed)
    }

    @Test
    fun `Corrections for another amp leave the panel's recalls alone`() {
        connected()
        panel.correct(LoudnessCorrections(AmpModels.grandMeisterDeluxe40, mapOf(3 to -11)))
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), nextSeen())
    }

    @Test
    fun `A preset recalled from the footswitch under a trim gets its volume trimmed`() {
        val volume = AmpModels.blackSpirit200.control("volume")!!
        connected()
        panel.trim(-20)
        fake.emit(ProgramChange(MidiChannel(1), 2))
        fake.emit(ValueBlock(mapOf(volume.controller to 120), preset = 3).message())
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), volume.controller, 40)), nextSeen())
        assertEquals(1, panel.trimmed)
    }

    @Test
    fun `A preset recalled with the tempo unlocked keeps its own delay time`() {
        val delay = AmpModels.blackSpirit200.control("delay_time")!!
        connected()
        panel.set(delay, 40)
        nextSeen()
        fake.emit(ValueBlock(mapOf(delay.controller to 141), preset = 3).message())
        until("delay time at 70") { it.state.values["delay_time"] == 70 }
        // An answer goes out as the block is taken in, so one would be on loopback well inside 200 ms.
        assertEquals(null, seen.poll(200, TimeUnit.MILLISECONDS))
    }

    /** The panel with modulation and delay on, reverb off and every knob behind them at 64. */
    private fun effectsOn() {
        val amp = AmpModels.blackSpirit200
        listOf("modulation" to 127, "delay" to 127, "reverb" to 0).forEach { (id, value) ->
            panel.set(amp.control(id)!!, value)
        }
        amp.controls.filter { it.needs == "modulation" || it.needs == "delay" }.forEach { panel.set(it, 64) }
    }

    @Test
    fun `Randomizing changes the effects that are on and leaves the rest alone`() {
        effectsOn()
        val before = panel.state.values
        panel.randomize(Random(5))
        val changed =
            panel.state.values
                .filter { (id, value) -> before[id] != value }
                .keys
        assertEquals(setOf("modulation", "delay"), panel.rollable.map { it.id }.toSet())
        assertTrue(
            changed.all {
                AmpModels.blackSpirit200.control(it)!!.needs in setOf("modulation", "delay")
            },
            "changed $changed",
        )
        assertEquals(
            before.filterKeys { it in setOf("modulation", "delay", "reverb") },
            panel.state.values.filterKeys {
                it in
                    setOf("modulation", "delay", "reverb")
            },
        )
    }

    @Test
    fun `Randomizing under a tempo lock keeps the delay time`() {
        effectsOn()
        panel.lock(AmpModels.blackSpirit200.control("delay_time")!!, on = true)
        repeat(20) { panel.randomize(Random(it)) }
        assertEquals(64, panel.state.values["delay_time"])
    }

    @Test
    fun `Undo puts back the values randomizing changed, once`() {
        effectsOn()
        val before = panel.state.values
        panel.randomize(Random(5))
        panel.undoRoll()
        assertEquals(before, panel.state.values)
        assertNull(panel.rolled)
    }

    @Test
    fun `A preset recalled after randomizing takes the undo away`() {
        effectsOn()
        panel.randomize(Random(5))
        assertNotNull(panel.rolled)
        panel.recall(3)
        assertNull(panel.rolled)
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
    fun `An amp picked before any box is remembered for the first box linked to`() {
        val remembered = ArrayList<Pair<WmiAddress, String>>()
        val box = WmiAddress("127.0.0.1", fake.port)
        val picking =
            PanelState(AmpModels.blackSpirit200, holder, scope, last, rememberAmp = { at, amp ->
                remembered +=
                    at to amp.id
            })
        picking.host = ""
        picking.choose(AmpModels.grandMeisterDeluxe40)
        assertEquals(emptyList(), remembered)
        picking.connect(box)
        until("remembered") { remembered.isNotEmpty() }
        assertEquals(box to "grandmeister_deluxe_40", remembered.first())
    }

    @Test
    fun `Linking to a box with no amp picked for it asks which amp it is, until one is picked`() {
        assertFalse(panel.asking)
        connected()
        until("asking") { it.asking }
        panel.choose(AmpModels.grandMeisterDeluxe40)
        assertFalse(panel.asking)
    }

    @Test
    fun `A box whose question was put away is not asked about again`() {
        connected()
        until("asking") { it.asking }
        panel.settle()
        panel.disconnect()
        until("idle") { it.link == LinkState.Idle }
        panel.connect()
        until("connected") { it.link is LinkState.Connected }
        assertFalse(panel.asking)
        assertEquals(AmpModels.blackSpirit200, panel.amp)
    }

    @Test
    fun `Before any link the panel shows the amp picked for the address in the fields`() {
        val box = WmiAddress("192.168.1.1", 1470)
        val starting =
            PanelState(
                AmpModels.blackSpirit200,
                holder,
                scope,
                last,
                MutableStateFlow(
                    mapOf(box to AmpModels.grandMeisterDeluxe40),
                ),
            )
        assertEquals(AmpModels.grandMeisterDeluxe40, starting.amp)
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

    @Test
    fun `A control moved with the cable open goes out on the cable`() {
        panel.connect(jack.doremidi)
        until("cable open") { it.linked }
        panel.set(gain, 100)
        val sent = assertNotNull(jack.sent.poll(5, TimeUnit.SECONDS), "nothing went out on the cable within 5 s")
        assertContentEquals(ControlChange(MidiChannel(1), gain.controller, 100).encode(), sent)
    }

    @Test
    fun `A knob the amp reports on the cable moves on the panel`() {
        panel.connect(jack.doremidi)
        until("cable open") { it.linked }
        jack.hear(ControlChange(MidiChannel(1), gain.controller, 42))
        until("gain at 42") { it.state.values["gain"] == 42 }
    }

    @Test
    fun `An unplugged cable is shown failed and leaves the panel idle with nothing sent`() {
        panel.connect(jack.doremidi)
        until("cable open") { it.linked }
        jack.unplug("unplugged")
        until("cable failed") { it.cable == CableState.Failed(jack.doremidi, "unplugged") }
        assertEquals(true, panel.idle)
        panel.set(gain, 100)
        assertEquals(null, jack.sent.poll())
    }

    @Test
    fun `Picking a cable drops the link to a WMI-1`() {
        connected()
        panel.connect(jack.doremidi)
        until("only the cable") { it.link == LinkState.Idle && it.cable == CableState.Open(jack.doremidi) }
    }

    /** One cable, always plugged in, that opens at once and keeps what it is sent. */
    private class PluggedJack : CableJack {
        val doremidi = Cable(7, "DOREMIDI")
        val sent = LinkedBlockingQueue<ByteArray>()
        var hear: (MidiMessage) -> Unit = {}
        var unplug: (String) -> Unit = {}

        override val cables: StateFlow<List<Cable>> = MutableStateFlow(listOf(doremidi))

        override fun open(
            cable: Cable,
            onMessage: (MidiMessage) -> Unit,
            onLost: (reason: String) -> Unit,
            onOpened: (Result<CableLink>) -> Unit,
        ) {
            hear = onMessage
            unplug = onLost
            onOpened(Result.success(CableLink(cable, write = sent::put, release = {})))
        }
    }
}
