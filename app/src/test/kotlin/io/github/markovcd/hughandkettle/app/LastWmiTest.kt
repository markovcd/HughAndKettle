package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.wmi.WmiAddress
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LastWmiTest {
    @Test
    fun `A remembered box reads back as itself`() {
        val box = WmiAddress("192.168.1.20", 2000)
        assertEquals(box, LastWmi.read(LastWmi.write(box)))
    }

    @Test
    fun `A version 1 value as it is written on phones reads`() {
        assertEquals(WmiAddress("wmi.local", 2001), LastWmi.read("1 wmi.local:2001"))
    }

    @Test
    fun `A value from a newer version is no last box`() {
        assertNull(LastWmi.read("2 192.168.1.20:2000"))
    }

    @Test
    fun `Nothing, or a value that does not read, is no last box`() {
        listOf(null, "", "garbage", "1", "1 :2000", "x 1.2.3.4:2000").forEach {
            assertNull(LastWmi.read(it), "from '$it'")
        }
    }
}
