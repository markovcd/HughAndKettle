package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongFile
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ValueBlock
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
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class TakeStateTest {
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
    private val songs = SongState(store, panel, scope, Dispatchers.Unconfined, click = { clicks.incrementAndGet() })

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

    private fun kept() = store.list(amp).getOrThrow().songs

    private fun countdown() = (songs.now as? SongState.Now.CountingIn)?.left

    /** Starts a take at 400 BPM with no count-in on [preset], and waits for its [beats]th beat. */
    private fun recording(
        preset: Int,
        beats: Int = 0,
    ) {
        connected()
        songs.tempo(400)
        songs.countIn(false)
        panel.recall(preset)
        next()
        songs.takes.record()
        until("beat $beats of the take") { songs.takes.at.let { it != null && it >= 150.milliseconds * beats } }
    }

    @Test
    fun `A take keeps each switch of sound as a step, and is kept at Stop as a song of its own, opened`() {
        recording(preset = 3, beats = 4)
        fake.emit(ValueBlock(emptyMap(), preset = 5).message())
        until("the footswitch recall taken") {
            songs.takes.take
                ?.marks
                ?.size == 2
        }
        until("two beats on") { songs.takes.at!! >= 150.milliseconds * 6 }
        songs.takes.stop()
        assertFalse(songs.takes.recording)
        assertEquals("Take 1", songs.song.name)
        assertEquals(listOf(3, 5), songs.song.steps.map { it.preset })
        assertEquals(400, songs.song.tempo)
        assertEquals("Take 1", songs.takes.kept)
        until("kept and opened") { store.list(amp).getOrThrow().open == "Take 1" }
    }

    @Test
    fun `A tone sent from the panel during a take is a step on that tone`() {
        recording(preset = 3, beats = 4)
        panel.recall(Tone("Lead", amp, emptyMap()))
        until("the tone taken") {
            songs.takes.take
                ?.marks
                ?.size == 2
        }
        songs.takes.stop()
        assertEquals(listOf(3 to null, null to "Lead"), songs.song.steps.map { it.preset to it.tone })
    }

    @Test
    fun `A discarded take is deleted and the song open before it opens again`() {
        songs.create("Opener")
        recording(preset = 3, beats = 1)
        songs.takes.stop()
        assertEquals("Take 1", songs.song.name)
        songs.takes.discard()
        assertEquals("Opener", songs.song.name)
        assertNull(songs.takes.kept)
        until("the take deleted") { kept().map { it.name } == listOf("Opener") }
    }

    @Test
    fun `During a take the song is not changed, played or swapped, and the sound switches as usual`() {
        songs.add()
        recording(preset = 3)
        songs.tempo(90)
        songs.start()
        songs.create("Other")
        assertEquals(400, songs.song.tempo)
        assertEquals(SongState.Now.Recording, songs.now)
        assertEquals("", songs.song.name)
        panel.step(1)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 3)), next())
        songs.takes.stop()
    }

    @Test
    fun `A take counting in reads as a take's count-in, and stopped there as stopped`() {
        connected()
        songs.tempo(20)
        songs.takes.record()
        assertEquals(SongState.Now.CountingIn(4, first = null, take = true), songs.now)
        songs.takes.stop()
        assertEquals(SongState.Now.Stopped, songs.now)
    }

    @Test
    fun `A take needs the amp linked, and one stopped in its count-in keeps nothing`() {
        songs.takes.record()
        assertFalse(songs.takes.recording)
        connected()
        songs.tempo(20)
        songs.takes.record()
        assertTrue(songs.takes.recording)
        songs.takes.stop()
        assertNull(songs.takes.kept)
        assertEquals(emptyList(), kept())
    }

    @Test
    fun `A take clicks every beat when the click while recording is on`() {
        songs.takes.recordClick(true)
        recording(preset = 3, beats = 3)
        songs.takes.stop()
        assertTrue(clicks.get() >= 3, "${clicks.get()} clicks")
    }
}
