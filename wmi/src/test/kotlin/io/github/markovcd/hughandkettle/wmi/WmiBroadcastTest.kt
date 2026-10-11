package io.github.markovcd.hughandkettle.wmi

import java.net.InetAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WmiBroadcastTest {
    private val from = InetAddress.getByName("192.168.4.1")

    private fun packet(
        port: Int,
        id: String,
        firmware: String = "wifly-EZX Ver 4.41",
    ): ByteArray {
        val packet = ByteArray(110)
        byteArrayOf(0x10, 0xFE.toByte(), 0xED.toByte(), 0x01, 0x02, 0x03).copyInto(packet, 0)
        packet[7] = (-60).toByte()
        packet[8] = (port shr 8).toByte()
        packet[9] = port.toByte()
        firmware.toByteArray().copyInto(packet, 32)
        id.toByteArray().copyInto(packet, 60)
        return packet
    }

    @Test
    fun `A status packet names the device, its MIDI port and its access point`() {
        val heard = WmiBroadcast.parse(packet(2000, "WMI-1_A1B2C3"), from = from)
        assertEquals(
            WmiBroadcast(from, 2000, "WMI-1_A1B2C3", "wifly-EZX Ver 4.41", "10:FE:ED:01:02:03", -60),
            heard,
        )
    }

    @Test
    fun `A packet too short to carry a device id is not a broadcast`() {
        assertNull(WmiBroadcast.parse(ByteArray(91), from = from))
        assertNull(WmiBroadcast.parse(packet(2000, "x"), length = 20, from = from))
    }

    @Test
    fun `The fake WMI-1 broadcasts a packet the parser reads back`() {
        FakeWmi(onEvent = {}).use { fake ->
            val heard = WmiBroadcast.parse(fake.statusPacket(), from = from)!!
            assertEquals(fake.port, heard.midiPort)
            assertEquals("WMI-1_FAKE", heard.deviceId)
        }
    }
}
