package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
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
import kotlinx.coroutines.flow.flowOf
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class SongStateTest {
    private val folder: Path = Files.createTempDirectory("songs")
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.blackSpirit200
    private val time = amp.control("delay_time")!!
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val store = SongStore(folder.resolve("songs"))
    private val clicks = AtomicInteger()
    private val songs = SongState(store, panel, scope, Dispatchers.Unconfined) { clicks.incrementAndGet() }

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
        folder.deleteRecursively()
    }

    private fun next(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    /** Waits, up to 5 s, for [holds] to come true. */
    private fun until(
        what: String,
        holds: () -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!holds()) {
            if (started.elapsedNow() > 5.seconds) fail("never got to: $what")
            Thread.sleep(5)
        }
    }

    private fun connected() {
        panel.host = "127.0.0.1"
        panel.port = "${fake.port}"
        panel.connect()
        until("connected") { panel.link == LinkState.Connected(WmiAddress("127.0.0.1", fake.port)) }
        assertEquals(FakeWmiEvent.Opened::class, next()::class)
    }

    @Test
    fun `A song edited on the phone is kept for its amp`() {
        songs.tempo(96)
        songs.add()
        songs.beats(0, 32)
        until("saved") { store.song(amp).getOrNull() == Song(amp, 96, listOf(SongStep(1, 32))) }
        songs.remove(0)
        until("step removed") { store.song(amp).getOrNull() == Song(amp, 96) }
    }

    @Test
    fun `A step moved up or down swaps places with its neighbor, and goes nowhere past the ends`() {
        songs.add()
        songs.beats(0, 8)
        songs.add()
        songs.move(1, 0)
        assertEquals(listOf(16, 8), songs.song.steps.map(SongStep::beats))
        songs.move(1, 2)
        assertEquals(listOf(16, 8), songs.song.steps.map(SongStep::beats))
    }

    @Test
    fun `Tapping a beat sets the song's tempo, from the second tap`() {
        songs.tap(0.milliseconds)
        assertEquals(120, songs.song.tempo)
        songs.tap(750.milliseconds)
        assertEquals(80, songs.song.tempo)
    }

    @Test
    fun `A song plays each preset on its beat with the delay time of its tempo, then lets the time go`() {
        connected()
        songs.tempo(400)
        songs.add()
        songs.beats(0, 2)
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        songs.add()
        songs.beats(1, 2)
        val delay = ControlChange(MidiChannel(1), time.controller, time.scale!!.value(150))
        songs.start()
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        assertEquals(time, panel.lock?.control)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        until("song over") { songs.position == null }
        assertNull(panel.lock)
    }

    @Test
    fun `A song counts in a bar of clicks before its first preset, and with the count-in off starts at once`() {
        connected()
        songs.tempo(400)
        songs.add()
        songs.start()
        assertEquals(4, songs.countdown)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(4, clicks.get())
        next()
        songs.stop()
        assertNull(songs.countdown)
        songs.countIn(false)
        songs.start()
        assertNull(songs.countdown)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(4, clicks.get())
        songs.stop()
    }

    @Test
    fun `A song stopped part way lets go of the time it held, and cannot be changed while it plays`() {
        songs.tempo(60)
        songs.add()
        connected()
        songs.start()
        next()
        next()
        songs.tempo(100)
        assertEquals(60, songs.song.tempo)
        songs.stop()
        assertNull(songs.position)
        assertNull(panel.lock)
    }
}
