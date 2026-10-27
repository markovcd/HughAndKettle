package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LfoShape
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class LfoStateTest {
    private val fake = FakeWmi(onEvent = {})
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.grandMeisterDeluxe40
    private var now = Duration.ZERO

    // The clock is the test's, and the loop ticks once at each start: the test calls every other tick.
    private val panel =
        PanelState(amp, holder, scope, flowOf(null), lfoClock = { now }, lfoPace = { awaitCancellation() })
    private val lfos = panel.lfos
    private val reverb = amp.control("reverb")!!
    private val delay = amp.control("delay")!!
    private val volume = amp.control("reverb_volume")!!
    private val time = amp.control("delay_time")!!

    @AfterTest
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
    }

    private fun until(
        what: String,
        holds: () -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!holds()) {
            if (started.elapsedNow() > 5.seconds) fail("never got to: $what (link is ${panel.link})")
            Thread.sleep(5)
        }
    }

    /** Linked, with reverb and delay on and their knobs at 50. */
    private fun linked() {
        panel.connect(WmiAddress("127.0.0.1", fake.port))
        until("connected") { panel.link is LinkState.Connected }
        listOf(reverb, delay).forEach { panel.set(it, 127) }
        amp.controls.filter { it.needs == "reverb" || it.needs == "delay" }.forEach { panel.set(it, 50) }
    }

    private fun heard(id: String) = panel.state.values[id]

    private fun tick(at: Duration) {
        now = at
        lfos.tick(at)
    }

    @Test
    fun `An LFO starts on its card's first level, swinging either side of where it is, and moves it each tick`() {
        linked()
        lfos.switch(reverb, on = true)
        val lfo = assertNotNull(lfos.running["reverb"])
        assertEquals(volume to (18 to 82), lfo.control to (lfo.low to lfo.high))
        assertEquals(18, heard("reverb_volume"))
        tick(1.seconds)
        assertEquals(82, heard("reverb_volume"))
    }

    @Test
    fun `An LFO waits while its effect is off and while a tone is written`() {
        linked()
        lfos.switch(reverb, on = true)
        panel.set(reverb, 0)
        tick(1.seconds)
        assertEquals(18, heard("reverb_volume"))
        panel.set(reverb, 127)
        panel.holding = true
        tick(1.seconds)
        assertEquals(18, heard("reverb_volume"))
        panel.holding = false
        tick(1.seconds)
        assertEquals(82, heard("reverb_volume"))
    }

    @Test
    fun `An LFO switched off puts its control back where it was`() {
        linked()
        lfos.switch(reverb, on = true)
        tick(1.seconds)
        lfos.switch(reverb, on = false)
        assertEquals(50, heard("reverb_volume"))
        assertNull(lfos.running["reverb"])
    }

    @Test
    fun `A card's LFO switched on again starts as it was left`() {
        linked()
        lfos.switch(reverb, on = true)
        lfos.set(reverb, lfos.running.getValue("reverb").copy(shape = LfoShape.Square, low = 0, high = 100))
        lfos.switch(reverb, on = false)
        lfos.switch(reverb, on = true)
        val lfo = lfos.running.getValue("reverb")
        assertEquals(LfoShape.Square to (0 to 100), lfo.shape to (lfo.low to lfo.high))
    }

    @Test
    fun `Moving an LFO to another control puts the first back and swings the new one around where it is`() {
        linked()
        lfos.switch(delay, on = true)
        assertEquals(
            "delay_volume",
            lfos.running
                .getValue("delay")
                .control.id,
        )
        tick(1.seconds)
        lfos.aim(delay, amp.control("delay_feedback")!!)
        assertEquals(50, heard("delay_volume"))
        val lfo = lfos.running.getValue("delay")
        assertEquals("delay_feedback" to (18 to 82), lfo.control.id to (lfo.low to lfo.high))
    }

    @Test
    fun `An LFO on the delay time turns the tempo lock off, and locking the time stops the LFO where it is`() {
        linked()
        panel.lock(time, on = true)
        lfos.switch(delay, on = true)
        lfos.aim(delay, time)
        assertNull(panel.lock)
        tick(1.seconds)
        val swept = heard("delay_time")
        panel.lock(time, on = true)
        assertNull(lfos.running["delay"])
        assertEquals(swept, heard("delay_time"))
        assertEquals(swept, panel.lock?.value)
    }

    @Test
    fun `Every LFO stops when the link goes`() {
        linked()
        lfos.switch(reverb, on = true)
        lfos.switch(delay, on = true)
        panel.disconnect()
        // The panel hears the link go on the link's own thread, a moment after the disconnect.
        until("every LFO stopped") { lfos.running.isEmpty() }
        assertEquals(LinkState.Idle, panel.link)
    }

    @Test
    fun `A different amp forgets every LFO`() {
        linked()
        lfos.switch(reverb, on = true)
        panel.choose(AmpModels.grandMeister36)
        assertTrue(lfos.running.isEmpty())
    }
}
