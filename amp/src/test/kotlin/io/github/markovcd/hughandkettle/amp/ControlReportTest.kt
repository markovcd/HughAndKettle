package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.SystemExclusive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ControlReportTest {
    /** A report as the amp sent it, F0 and F7 left off. */
    private fun heard(hex: String): ControlReport? =
        ControlReport.parse(SystemExclusive(hex.split(' ').map { it.toInt(16).toByte() }.toByteArray()))

    @Test
    fun `Reports captured from a GrandMeister read as controller and value`() {
        val captured =
            mapOf(
                "00 20 44 00 10 00 0A 00 04 09 00 7F 7C" to ControlReport(9, 127),
                "00 20 44 00 10 00 0A 00 04 40 00 00 4A" to ControlReport(64, 0),
                "00 20 44 00 10 00 0A 00 04 38 01 4E 7D" to ControlReport(56, 206),
                "00 20 44 00 10 00 0A 00 04 04 01 7F 70" to ControlReport(4, 255),
                "00 20 44 00 10 00 0A 00 04 1E 00 5F 4B" to ControlReport(30, 95),
            )
        assertEquals(captured, captured.mapValues { (hex, _) -> heard(hex) })
    }

    @Test
    fun `A report whose checksum does not match is no report`() {
        assertNull(heard("00 20 44 00 10 00 0A 00 04 09 00 7F 7D"))
    }

    @Test
    fun `Other system exclusives are no report`() {
        assertNull(heard("7E 00 06 02 00 20 44 10 00 09 00 31 2E 31 36"))
        assertNull(heard("00 20 44 00 10 00 0A 00 50 10 00 20 01 6F"))
    }
}
