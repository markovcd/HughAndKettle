package io.github.markovcd.hughandkettle.wmi

import java.net.InetAddress

/**
 * One status broadcast from a WMI-1, which is how the app finds it on the network.
 *
 * The layout is the RN-171 UDP broadcast packet (WiFly command reference, figure 3-12): 110 bytes,
 * or 120 when the module appends its own MAC and IP.
 */
public data class WmiBroadcast(
    val address: InetAddress,
    val midiPort: Int,
    val deviceId: String,
    val firmware: String,
    val accessPointMac: String,
    val rssi: Int,
) {
    public companion object {
        public const val MIN_LENGTH: Int = 92
        private const val AP_MAC = 0
        private const val RSSI = 7
        private const val PORT = 8
        private const val FIRMWARE = 32
        private const val FIRMWARE_LENGTH = 28
        private const val DEVICE_ID = 60
        private const val DEVICE_ID_LENGTH = 32

        /** Reads a packet from [from]; null when it is too short to be a module broadcast. */
        public fun parse(
            packet: ByteArray,
            length: Int = packet.size,
            from: InetAddress,
        ): WmiBroadcast? {
            if (length < MIN_LENGTH || length > packet.size) return null
            return WmiBroadcast(
                address = from,
                midiPort = ((packet[PORT].toInt() and 0xFF) shl 8) or (packet[PORT + 1].toInt() and 0xFF),
                deviceId = packet.text(DEVICE_ID, DEVICE_ID_LENGTH),
                firmware = packet.text(FIRMWARE, FIRMWARE_LENGTH),
                accessPointMac =
                    (AP_MAC until AP_MAC + 6).joinToString(
                        ":",
                    ) { "%02X".format(packet[it].toInt() and 0xFF) },
                rssi = packet[RSSI].toInt(),
            )
        }

        private fun ByteArray.text(
            offset: Int,
            length: Int,
        ): String {
            val end = (offset until offset + length).firstOrNull { this[it] == 0.toByte() } ?: (offset + length)
            return String(this, offset, end - offset, Charsets.US_ASCII).trim()
        }
    }
}
