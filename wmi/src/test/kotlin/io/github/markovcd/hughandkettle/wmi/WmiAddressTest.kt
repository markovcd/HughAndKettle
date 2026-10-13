package io.github.markovcd.hughandkettle.wmi

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WmiAddressTest {
    @Test
    fun `A bare host gets the WMI-1 default port`() {
        assertEquals(WmiAddress("1.2.3.4", 1470), WmiAddress.parse("1.2.3.4"))
    }

    @Test
    fun `A host with a port keeps it`() {
        assertEquals(WmiAddress("wmi.local", 2001), WmiAddress.parse("wmi.local:2001"))
    }

    @Test
    fun `A port that is not a number or out of range is nothing`() {
        assertNull(WmiAddress.parse("host:abc"))
        assertNull(WmiAddress.parse("host:70000"))
        assertNull(WmiAddress.parse(":2000"))
    }

    @Test
    fun `An address prints the way it is typed`() {
        assertEquals("wmi.local:2001", WmiAddress("wmi.local", 2001).toString())
    }
}
