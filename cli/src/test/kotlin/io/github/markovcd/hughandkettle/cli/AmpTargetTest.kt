package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class AmpTargetTest {
    @Test
    fun `A host names a WMI-1 and cable with a name names a cable`() {
        assertEquals(
            mapOf(
                "192.168.1.1" to AmpTarget.Wmi(WmiAddress("192.168.1.1", WmiDefaults.MIDI_PORT)),
                "127.0.0.1:2000" to AmpTarget.Wmi(WmiAddress("127.0.0.1", 2000)),
                "cable:CH345" to AmpTarget.Cable("CH345"),
                "cable:" to null,
                "cable: " to null,
            ),
            listOf(
                "192.168.1.1",
                "127.0.0.1:2000",
                "cable:CH345",
                "cable:",
                "cable: ",
            ).associateWith(AmpTarget::parse),
        )
    }
}
