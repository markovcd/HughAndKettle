package io.github.markovcd.hughandkettle.midi

import kotlin.test.Test
import kotlin.test.assertEquals

class MidiStreamParserTest {
    private fun heard(vararg chunks: String): List<MidiMessage> {
        val parser = MidiStreamParser()
        return chunks.flatMap { parser.feed(it.hexToBytes()!!) }
    }

    @Test
    fun `A control change and a program change read back as themselves`() {
        assertEquals(
            listOf(ControlChange(MidiChannel(1), 20, 100), ProgramChange(MidiChannel(3), 7)),
            heard("B0 14 64 C2 07"),
        )
    }

    @Test
    fun `A message split across chunks is still one message`() {
        assertEquals(listOf(ControlChange(MidiChannel(1), 20, 100)), heard("B0", "14", "64"))
    }

    @Test
    fun `Running status repeats the last status byte`() {
        assertEquals(
            listOf(ControlChange(MidiChannel(1), 20, 1), ControlChange(MidiChannel(1), 21, 2)),
            heard("B0 14 01 15 02"),
        )
    }

    @Test
    fun `A real-time byte inside a message does not break it`() {
        assertEquals(
            listOf(SystemRealtime(0xFE), ControlChange(MidiChannel(1), 20, 100)),
            heard("B0 FE 14 64"),
        )
    }

    @Test
    fun `A text banner before the first status byte is dropped`() {
        val banner = "*HELLO*".toByteArray().toHex()
        assertEquals(listOf(ControlChange(MidiChannel(1), 9, 127)), heard("$banner B0 09 7F"))
    }

    @Test
    fun `A system exclusive carries what sits between F0 and F7`() {
        assertEquals(listOf(SystemExclusive("00 20 44 01".hexToBytes()!!)), heard("F0 00 20 44 01 F7"))
    }

    @Test
    fun `A status byte ends a system exclusive that has no F7`() {
        assertEquals(
            listOf(SystemExclusive("01 02".hexToBytes()!!), ProgramChange(MidiChannel(1), 5)),
            heard("F0 01 02 C0 05"),
        )
    }

    @Test
    fun `A system exclusive longer than the cap costs nothing and yields nothing`() {
        val parser = MidiStreamParser(maxSysExBytes = 4)
        val heard = parser.feed("F0 01 02 03 04 05 06 F7 C0 01".hexToBytes()!!)
        assertEquals(listOf(ProgramChange(MidiChannel(1), 1)), heard)
    }

    @Test
    fun `A note is kept as another message so a log can show it`() {
        assertEquals(listOf(OtherMessage("90 3C 40".hexToBytes()!!)), heard("90 3C 40"))
    }

    @Test
    fun `A song position carries two data bytes and no running status`() {
        assertEquals(
            listOf(OtherMessage("F2 01 02".hexToBytes()!!)),
            heard("F2 01 02 03 04"),
        )
    }

    @Test
    fun `Every message encodes to bytes that parse back to the same message`() {
        val messages =
            listOf(
                ControlChange(MidiChannel(16), 127, 0),
                ProgramChange(MidiChannel(1), 127),
                SystemExclusive("7E 7F 09 01".hexToBytes()!!),
                SystemRealtime(0xF8),
                OtherMessage("E0 00 40".hexToBytes()!!),
            )
        val parser = MidiStreamParser()
        val failures = messages.filter { parser.feed(it.encode()) != listOf(it) }
        assertEquals(emptyList(), failures)
    }
}
