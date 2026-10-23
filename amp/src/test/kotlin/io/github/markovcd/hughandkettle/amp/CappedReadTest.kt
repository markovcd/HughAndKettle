package io.github.markovcd.hughandkettle.amp

import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertNull

class CappedReadTest {
    @Test
    fun `A stream up to the cap reads whole`() {
        val bytes = ByteArray(20_000) { it.toByte() }
        assertContentEquals(bytes, CappedRead.bytes(ByteArrayInputStream(bytes), 20_000))
    }

    @Test
    fun `A stream past the cap reads as nothing`() {
        assertNull(CappedRead.bytes(ByteArrayInputStream(ByteArray(20_001)), 20_000))
    }
}
