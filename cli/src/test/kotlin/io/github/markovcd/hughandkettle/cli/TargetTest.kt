package io.github.markovcd.hughandkettle.cli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TargetTest {
    @Test
    fun `A bare host gets the WMI-1 default port`() {
        assertEquals(Target("1.2.3.4", 2000), Target.parse("1.2.3.4"))
    }

    @Test
    fun `A host with a port keeps it`() {
        assertEquals(Target("wmi.local", 2001), Target.parse("wmi.local:2001"))
    }

    @Test
    fun `A port that is not a number or out of range is nothing`() {
        assertNull(Target.parse("host:abc"))
        assertNull(Target.parse("host:70000"))
        assertNull(Target.parse(":2000"))
    }
}
