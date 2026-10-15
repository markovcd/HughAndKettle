package io.github.markovcd.hughandkettle.midi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JavaCablesTest {
    @Test
    fun `A message spelled the way Java's MIDI spells it keeps its bytes`() {
        val messages =
            listOf(
                ControlChange(MidiChannel(1), 9, 127),
                ControlChange(MidiChannel(16), 31, 0),
                ProgramChange(MidiChannel(3), 5),
                SystemExclusive(byteArrayOf(0x7D, 0x01, 0x02)),
                SystemRealtime(0xF8),
            )
        val wrong = messages.filterNot { JavaCables.toJava(it).message.contentEquals(it.encode()) }
        assertEquals(emptyList(), wrong)
    }
}
