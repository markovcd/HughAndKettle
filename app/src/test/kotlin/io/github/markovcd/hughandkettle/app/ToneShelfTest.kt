package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.ToneFile
import io.github.markovcd.hughandkettle.amp.ToneStore
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
import org.junit.After
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class ToneShelfTest {
    private val folder: Path = Files.createTempDirectory("shelf")
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.blackSpirit200
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val shelf = ToneShelf(ToneStore(folder.resolve("tones")), panel, scope, Dispatchers.Unconfined)

    @After
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
        folder.deleteRecursively()
    }

    private fun names() = shelf.tones.map { it.name }

    private fun connect(port: Int) {
        panel.host = "127.0.0.1"
        panel.port = "$port"
        panel.connect()
        val started = TimeSource.Monotonic.markNow()
        while (panel.link != LinkState.Connected(WmiAddress("127.0.0.1", port))) {
            if (started.elapsedNow() > 5.seconds) fail("never connected: ${panel.link}")
            Thread.sleep(5)
        }
    }

    private fun set(vararg values: Pair<String, Int>) =
        values.forEach { (id, value) -> panel.set(amp.control(id)!!, value) }

    @Test
    fun `The panel's state saves as a tone holding what the panel knows`() {
        set("gain" to 40, "channel" to 79)
        shelf.save("  Lead ")
        assertEquals(listOf(Tone("Lead", amp, mapOf("gain" to 40, "channel" to 79))), shelf.tones)
    }

    @Test
    fun `With nothing known there is nothing to save`() {
        assertEquals(false, shelf.canSave)
        shelf.save("Empty")
        assertEquals(emptyList(), shelf.tones)
    }

    @Test
    fun `A second tone of the same name is refused with a notice`() {
        set("gain" to 40)
        shelf.save("Lead")
        shelf.save("lead")
        assertEquals(listOf("Lead"), names())
        assertTrue(shelf.notice is ToneNotice.Failed, "${shelf.notice}")
    }

    @Test
    fun `A recalled tone reaches the WMI-1 in panel order and shows on the panel`() {
        panel.host = "127.0.0.1"
        panel.port = "${fake.port}"
        panel.connect()
        val started = TimeSource.Monotonic.markNow()
        while (panel.link != LinkState.Connected(WmiAddress("127.0.0.1", fake.port))) {
            if (started.elapsedNow() > 5.seconds) fail("never connected: ${panel.link}")
            Thread.sleep(5)
        }
        shelf.recall(Tone("Lead", amp, mapOf("volume" to 90, "gain" to 100, "channel" to 79)))
        val received =
            generateSequence {
                seen.poll(5, TimeUnit.SECONDS)
            }.filterIsInstance<FakeWmiEvent.Received>().take(3).toList()
        assertEquals(listOf(31, 20, 7), received.map { (it.message as ControlChange).controller })
        assertEquals(mapOf("volume" to 90, "gain" to 100, "channel" to 79), panel.state.values)
    }

    @Test
    fun `A tone for another amp is listed but not recalled`() {
        val other = Tone("Deluxe", AmpModels.grandMeisterDeluxe40, mapOf("gain" to 1))
        shelf.import { ByteArrayInputStream(ToneFile.write(other).toByteArray()) }
        assertEquals(listOf("Deluxe"), names())
        assertEquals(false, shelf.fits(other))
        shelf.recall(other)
        assertEquals(emptyMap(), panel.state.values)
    }

    @Test
    fun `Tones rename, move by a place, and delete`() {
        set("gain" to 1)
        listOf("A", "B", "C").forEach(shelf::save)
        shelf.rename(shelf.tones[0], "Verse")
        shelf.move(shelf.tones[2], -1)
        shelf.move(shelf.tones[0], -1)
        assertEquals(listOf("Verse", "C", "B"), names())
        shelf.delete(shelf.tones[1])
        assertEquals(listOf("Verse", "B"), names())
    }

    @Test
    fun `An exported tone imports onto another phone's shelf as the same tone`() {
        set("gain" to 40, "mid" to 3)
        shelf.save("Lead")
        val file = ByteArrayOutputStream()
        shelf.export(shelf.tones.single()) { file }
        val other = ToneShelf(ToneStore(folder.resolve("other")), panel, scope, Dispatchers.Unconfined)
        other.import { ByteArrayInputStream(file.toByteArray()) }
        assertEquals(shelf.tones, other.tones)
        assertEquals(ToneNotice.Imported("Lead", emptyList()), other.notice)
    }

    @Test
    fun `An import reports what it left out, and refuses a newer file or one too large`() {
        val odd = """{"version": 1, "amp": "black_spirit_200", "name": "Odd", "values": {"gain": 3, "power_soak": 1}}"""
        shelf.import { ByteArrayInputStream(odd.toByteArray()) }
        assertEquals(ToneNotice.Imported("Odd", listOf("Black Spirit 200 has no control 'power_soak'")), shelf.notice)
        val newer = """{"version": 2, "amp": "black_spirit_200", "name": "New", "values": {}}"""
        shelf.import { ByteArrayInputStream(newer.toByteArray()) }
        assertTrue((shelf.notice as ToneNotice.Failed).reason.contains("newer Hugh & Kettle"), "${shelf.notice}")
        shelf.import { ByteArrayInputStream(ByteArray(1 shl 20) { ' '.code.toByte() }) }
        assertTrue((shelf.notice as ToneNotice.Failed).reason.contains("larger than a tone file"), "${shelf.notice}")
        assertEquals(listOf("Odd"), names())
    }

    @Test
    fun `Reading the presets keeps each as a tone named after it and returns to the preset the amp was on`() {
        lateinit var amp: FakeWmi
        lateinit var reader: ToneShelf
        val recalled = LinkedBlockingQueue<Int>()
        // A stand-in amp that answers a recall with its value block but sends nothing for preset 3;
        // the reading is stopped as preset 4 is recalled, so it ends after reading that one.
        amp =
            FakeWmi(onEvent = { event ->
                val program = ((event as? FakeWmiEvent.Received)?.message as? ProgramChange)?.program ?: return@FakeWmi
                recalled.put(program + 1)
                if (program == 3) reader.stopReading()
                if (program != 2) amp.emit(ValueBlock(mapOf(21 to 2 * program), program + 1).message())
            })
        amp.use {
            reader =
                ToneShelf(
                    ToneStore(folder.resolve("read")),
                    panel,
                    scope,
                    Dispatchers.Unconfined,
                    holder.heard,
                    names = { PresetNames(this.amp, mapOf(2 to "Lead")) },
                )
            connect(amp.port)
            panel.recall(7)
            assertEquals(7, recalled.poll(5, TimeUnit.SECONDS))
            assertEquals(true, reader.canRead)
            reader.readPresets()
            val started = TimeSource.Monotonic.markNow()
            while (reader.reading != null || reader.notice == null || reader.tones.size < 3) {
                if (started.elapsedNow() >
                    10.seconds
                ) {
                    fail("never finished: at ${reader.reading?.preset}, heard $recalled, kept ${reader.tones}")
                }
                Thread.sleep(5)
            }
            assertEquals(
                listOf(1, 2, 3, 4, 7),
                generateSequence { recalled.poll(5, TimeUnit.SECONDS) }.take(5).toList(),
            )
            assertEquals(listOf("Preset 1", "2 Lead", "Preset 4"), reader.tones.map { it.name })
            assertEquals(listOf(0, 1, 3), reader.tones.map { it.values["bass"] })
            assertEquals(ToneNotice.Read(3, listOf(3)), reader.notice)
        }
    }
}
