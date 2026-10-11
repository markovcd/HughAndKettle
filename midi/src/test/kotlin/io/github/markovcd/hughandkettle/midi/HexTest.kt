package io.github.markovcd.hughandkettle.midi

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HexTest {
    @Test
    fun `Bytes print as upper-case pairs separated by spaces`() {
        assertEquals("B0 14 7F", byteArrayOf(0xB0.toByte(), 0x14, 0x7F).toHex())
    }

    @Test
    fun `Printed hex reads back with or without spaces`() {
        assertContentEquals(byteArrayOf(0xB0.toByte(), 0x14), "b014".hexToBytes())
        assertContentEquals(byteArrayOf(0xB0.toByte(), 0x14), "B0 14".hexToBytes())
    }

    @Test
    fun `Text that is not hex pairs reads as nothing rather than throwing`() {
        assertNull("B0 1".hexToBytes())
        assertNull("zz".hexToBytes())
    }
}
