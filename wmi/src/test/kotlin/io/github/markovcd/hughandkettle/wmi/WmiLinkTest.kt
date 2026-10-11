package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.OtherMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

class WmiLinkTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)

    @AfterTest
    fun teardown() = fake.close()

    private fun next(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    private fun opened(onMessage: (MidiMessage) -> Unit = {}): WmiLink =
        WmiLink.open("127.0.0.1", fake.port, onMessage = onMessage).getOrThrow().also { next() }

    @Test
    fun `A message sent over the link arrives whole at the WMI-1`() {
        opened().use { link ->
            link.send(ControlChange(MidiChannel(1), 20, 100)).getOrThrow()
            assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), 20, 100)), next())
        }
    }

    @Test
    fun `A message the amp sends arrives at the app`() {
        val heard = LinkedBlockingQueue<MidiMessage>()
        opened(heard::put).use {
            assertTrue(fake.emit(ProgramChange(MidiChannel(1), 3)))
            assertEquals(ProgramChange(MidiChannel(1), 3), heard.poll(5, TimeUnit.SECONDS))
        }
    }

    @Test
    fun `Every byte the WMI-1 sends is handed over raw, a banner the parser drops included`() {
        val chunks = LinkedBlockingQueue<ByteArray>()
        val heard = LinkedBlockingQueue<MidiMessage>()
        WmiLink.open("127.0.0.1", fake.port, onMessage = heard::put, onBytes = chunks::put).getOrThrow().use {
            next()
            assertTrue(fake.emit(OtherMessage("*HELLO*".toByteArray(Charsets.US_ASCII))))
            assertTrue(fake.emit(ProgramChange(MidiChannel(1), 3)))
            assertEquals(ProgramChange(MidiChannel(1), 3), heard.poll(5, TimeUnit.SECONDS))
            val raw = generateSequence { chunks.poll() }.fold(ByteArray(0)) { all, chunk -> all + chunk }
            assertEquals("*HELLO*\u00C0\u0003", String(raw, Charsets.ISO_8859_1))
        }
    }

    @Test
    fun `A host that does not answer is a failure value`() {
        val result = WmiLink.open("127.0.0.1", 1, timeout = 200.milliseconds)
        assertTrue(result.isFailure)
    }

    @Test
    fun `Closing the link is reported to the WMI-1 and to the app once`() {
        val reasons = LinkedBlockingQueue<String>()
        val link = opened()
        link.close()
        assertEquals(FakeWmiEvent.Closed, next())
        // The app side's onClosed fires with no reason when it closed the link itself.
        WmiLink
            .open(
                "127.0.0.1",
                fake.port,
                onClosed = { reasons.put(it ?: "self") },
            ).getOrThrow()
            .also { next() }
            .close()
        assertEquals("self", reasons.poll(5, TimeUnit.SECONDS))
    }
}
