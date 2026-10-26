package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongFile
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.Tone
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

    @Test
    fun `A song edited on the phone is kept for its amp`() {
        songs.tempo(96)
        songs.add()
        songs.beats(0, 32)
        until("saved") { kept().singleOrNull()?.song == Song(amp, 96, listOf(SongStep(1, 32))) }
        songs.remove(0)
        until("step removed") { kept().singleOrNull()?.song == Song(amp, 96) }
    }

    @Test
    fun `An untitled song is kept only once it has a step or a name`() {
        songs.tempo(96)
        assertEquals(emptyList(), kept())
        assertEquals(96, songs.song.tempo)
        songs.rename("", "Set opener")
        until("kept by its name") { kept().map { it.name } == listOf("Set opener") }
        assertEquals("Set opener", songs.song.name)
        assertEquals("Set opener", store.list(amp).getOrThrow().open)
    }

    @Test
    fun `A new song opens empty, and the song before is there to open again`() {
        songs.add()
        songs.create("Encore")
        assertEquals(Song(amp, name = "Encore"), songs.song)
        assertEquals(true, songs.taken("encore"))
        songs.open("")
        assertEquals(1, songs.song.steps.size)
        until("open kept") { store.list(amp).getOrThrow().open == "" }
    }

    @Test
    fun `Songs are renamed, duplicated, moved and deleted, and the open one deleted gives way to the first left`() {
        songs.create("A")
        songs.add()
        songs.create("B")
        songs.rename("B", "Bee")
        assertEquals("Bee", songs.song.name)
        songs.duplicate("A")
        songs.move("Bee", -2)
        assertEquals(listOf("Bee", "A", "A 2"), songs.songs.map { it.name })
        songs.delete("Bee")
        assertEquals(listOf("A", "A 2"), songs.songs.map { it.name })
        assertEquals("A", songs.song.name)
        assertEquals(
            1,
            songs.songs[1]
                .song
                ?.steps
                ?.size,
        )
    }

    @Test
    fun `A song exported imports back named apart, and one for another amp is refused whole`() {
        songs.create("Encore")
        songs.add()
        val file = ByteArrayOutputStream()
        songs.export("Encore") { file }
        songs.import { ByteArrayInputStream(file.toByteArray()) }
        assertEquals(listOf("Encore", "Encore 2"), songs.songs.map { it.name })
        assertEquals(SongNotice.ImportedAs("Encore", "Encore 2"), songs.notice)
        val other = SongFile.write(Song(AmpModels.grandMeister36, name = "Solo"))
        songs.import { ByteArrayInputStream(other.toByteArray()) }
        assertEquals(SongNotice.OtherAmp(AmpModels.grandMeister36.name), songs.notice)
        assertEquals(2, songs.songs.size)
    }

    @Test
    fun `The single song an older version kept opens as Song 1 and says so`() {
        val older = SongStore(folder.resolve("old"))
        folder.resolve("old").createDirectories()
        folder.resolve("old/${amp.id}.json").writeText(
            """{ "version": 1, "amp": "${amp.id}", "tempo": 140, "steps": [ { "preset": 2, "beats": 8 } ] }""",
        )
        val carried = SongState(older, panel, scope, Dispatchers.Unconfined)
        until("carried over") { carried.song.name == "Song 1" }
        assertEquals(Song(amp, 140, listOf(SongStep(2, 8)), name = "Song 1"), carried.song)
        assertEquals(SongNotice.Carried("Song 1"), carried.notice)
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
    fun `While the tempo lock is on, a song plays each preset on its beat with the delay time of its tempo`() {
        connected()
        songs.tempo(400)
        songs.add()
        songs.beats(0, 2)
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        songs.add()
        songs.beats(1, 2)
        panel.lock(time, on = true)
        val delay = ControlChange(MidiChannel(1), time.controller, time.scale!!.value(150))
        songs.start()
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        until("song over") { songs.position == null }
        assertEquals(time, panel.lock?.control)
    }

    @Test
    fun `A step with a note of its own plays the delay at it, and the steps without at the note picked`() {
        connected()
        songs.tempo(400)
        songs.add()
        songs.beats(0, 2)
        songs.note(0, NoteLength.of("1/2"))
        songs.add()
        songs.beats(1, 2)
        assertEquals(NoteLength.of("1/2"), songs.song.steps[0].note)
        songs.lock(on = true)
        val quarter = ControlChange(MidiChannel(1), time.controller, time.scale!!.value(150))
        assertEquals(FakeWmiEvent.Received(quarter), next())
        songs.start()
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        val half = ControlChange(MidiChannel(1), time.controller, time.scale!!.value(300))
        assertEquals(FakeWmiEvent.Received(half), next())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(FakeWmiEvent.Received(quarter), next())
        until("song over") { songs.position == null }
        assertEquals(NoteLength.QUARTER, songs.sounding)
    }

    @Test
    fun `While locked, the delay time is the note at the song's tempo, moved with either`() {
        connected()
        songs.lock(on = true)
        assertEquals(
            FakeWmiEvent.Received(ControlChange(MidiChannel(1), time.controller, time.scale!!.value(500))),
            next(),
        )
        songs.note(NoteLength.of("1/8d")!!)
        assertEquals(
            FakeWmiEvent.Received(ControlChange(MidiChannel(1), time.controller, time.scale!!.value(375))),
            next(),
        )
        songs.tempo(100)
        assertEquals(
            FakeWmiEvent.Received(ControlChange(MidiChannel(1), time.controller, time.scale!!.value(450))),
            next(),
        )
    }

    @Test
    fun `A delay time the amp sets itself while locked sets the song's tempo instead`() {
        connected()
        songs.lock(on = true)
        next()
        val tapped = time.scale!!.value(476)
        fake.emit(ControlChange(MidiChannel(1), time.controller, tapped))
        val bpm = NoteLength.QUARTER.tempo(time.scale!!.amount(tapped))
        until("tempo from the amp") { songs.song.tempo == bpm }
        assertTrue(songs.tempoFromAmp)
        assertEquals(tapped, panel.lock?.value)
        // An answer would go out as the change is taken in, so one would be on loopback well inside 200 ms.
        assertNull(seen.poll(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `With the tempo lock off, a song plays each preset on its beat and leaves the delay time to the preset`() {
        connected()
        songs.tempo(400)
        songs.countIn(false)
        songs.add()
        songs.beats(0, 2)
        panel.recall(3)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        songs.add()
        songs.beats(1, 2)
        assertFalse(songs.timed)
        songs.start()
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        until("song over") { songs.position == null }
        assertNull(panel.lock)
    }

    @Test
    fun `While locked, a song plays a tone step as its values, and a missing tone's as the delay time alone`() {
        val gain = amp.control("gain")!!
        val lead = Tone("Lead", amp, mapOf("gain" to 90))
        var shelf = listOf(lead, Tone("Gone", amp, emptyMap()))
        val songs = SongState(store, panel, scope, Dispatchers.Unconfined, tones = { shelf })
        connected()
        songs.tempo(400)
        songs.countIn(false)
        songs.add(lead)
        songs.add(shelf[1])
        listOf(0, 1).forEach { songs.beats(it, 2) }
        shelf = listOf(lead)
        assertEquals(listOf(false, true), songs.song.steps.map(songs::missing))
        panel.lock(time, on = true)
        val delay = ControlChange(MidiChannel(1), time.controller, time.scale!!.value(150))
        songs.start()
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), gain.controller, 90)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        until("song over") { songs.position == null }
    }

    @Test
    fun `A tone renamed is renamed in the steps of the open song`() {
        val lead = Tone("Lead", amp, mapOf("gain" to 90))
        songs.add(lead)
        until("saved") {
            kept()
                .singleOrNull()
                ?.song
                ?.steps
                ?.singleOrNull()
                ?.tone == "Lead"
        }
        songs.renameTone(lead, "Solo")
        until("renamed") {
            songs.song.steps
                .singleOrNull()
                ?.tone == "Solo"
        }
        assertEquals(
            "Solo",
            kept()
                .single()
                .song
                ?.steps
                ?.single()
                ?.tone,
        )
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
    fun `A count-in plays its clicks the sound output's lag ahead, so the first preset comes that much later`() {
        val heard = ArrayList<Duration>()
        val began = TimeSource.Monotonic.markNow()
        val lagging =
            SongState(
                store,
                panel,
                scope,
                Dispatchers.Unconfined,
                { heard += began.elapsedNow() },
                lag = { 300.milliseconds },
            )
        connected()
        lagging.tempo(400)
        lagging.add()
        lagging.start()
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        val switched = began.elapsedNow()
        lagging.stop()
        // A bar at 400 BPM is 600 ms and the clicks went 300 ms ahead of it; delay rounds to whole milliseconds.
        assertEquals(4, heard.size)
        assertTrue(
            switched - heard.first() >= 890.milliseconds,
            "the preset came ${switched - heard.first()} after the first click",
        )
    }

    @Test
    fun `A song step under a trim recalls its preset behind the mute, as a tapped recall does`() {
        val mute = amp.control("mute")!!
        connected()
        panel.trim(-20)
        songs.tempo(400)
        songs.countIn(false)
        songs.add()
        songs.start()
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), mute.controller, 127)), next())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 0)), next())
        songs.stop()
    }

    @Test
    fun `A song stopped part way leaves the tempo lock on, and no song is changed or opened while it plays`() {
        songs.tempo(60)
        songs.add()
        connected()
        panel.lock(time, on = true)
        songs.start()
        next()
        next()
        songs.tempo(100)
        assertEquals(60, songs.song.tempo)
        songs.create("Other")
        assertEquals("", songs.song.name)
        songs.stop()
        assertNull(songs.position)
        assertEquals(time, panel.lock?.control)
    }
}
