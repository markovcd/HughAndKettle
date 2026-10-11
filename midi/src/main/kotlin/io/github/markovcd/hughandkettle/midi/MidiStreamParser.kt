package io.github.markovcd.hughandkettle.midi

/**
 * Turns a byte stream into messages, however the bytes are chunked.
 *
 * The stream is hostile until checked: the WMI-1 may prefix text banners, a chunk may split a message,
 * the amp may use running status, and real-time bytes may land anywhere. Stray data bytes with no
 * status to belong to are dropped, and a system exclusive longer than [maxSysExBytes] is dropped
 * rather than buffered without bound.
 */
public class MidiStreamParser(
    private val maxSysExBytes: Int = 65_536,
) {
    private var runningStatus = 0
    private val data = ArrayList<Byte>(3)
    private var sysEx: ArrayList<Byte>? = null
    private var sysExOverflowed = false

    /** Feeds a chunk and returns every message it completed, in order. */
    public fun feed(bytes: ByteArray): List<MidiMessage> {
        val out = ArrayList<MidiMessage>()
        for (byte in bytes) feed(byte.toInt() and 0xFF, out)
        return out
    }

    private fun feed(
        byte: Int,
        out: MutableList<MidiMessage>,
    ) {
        when {
            byte >= 0xF8 -> out += SystemRealtime(byte)
            byte == 0xF0 -> startSysEx(out)
            byte == 0xF7 -> endSysEx(out)
            byte >= 0x80 -> startMessage(byte, out)
            sysEx != null -> appendSysEx(byte)
            runningStatus != 0 -> appendData(byte, out)
            // A data byte with nothing to belong to: a banner, or the tail of a message cut before the link opened.
        }
    }

    private fun startSysEx(out: MutableList<MidiMessage>) {
        endSysEx(out)
        sysEx = ArrayList()
        sysExOverflowed = false
        runningStatus = 0
        data.clear()
    }

    private fun appendSysEx(byte: Int) {
        val buffer = sysEx ?: return
        if (buffer.size >= maxSysExBytes) {
            sysExOverflowed = true
            buffer.clear()
            return
        }
        buffer += byte.toByte()
    }

    private fun endSysEx(out: MutableList<MidiMessage>) {
        val buffer = sysEx ?: return
        if (!sysExOverflowed) out += SystemExclusive(buffer.toByteArray())
        sysEx = null
    }

    private fun startMessage(
        status: Int,
        out: MutableList<MidiMessage>,
    ) {
        // Any status byte ends a system exclusive that forgot its F7.
        endSysEx(out)
        data.clear()
        if (dataLength(status) == 0) {
            out += OtherMessage(byteArrayOf(status.toByte()))
            runningStatus = 0
        } else {
            runningStatus = status
        }
    }

    private fun appendData(
        byte: Int,
        out: MutableList<MidiMessage>,
    ) {
        data += byte.toByte()
        if (data.size < dataLength(runningStatus)) return
        out += complete(runningStatus, data.toByteArray())
        data.clear()
        // System common messages carry no running status.
        if (runningStatus >= 0xF0) runningStatus = 0
    }

    private fun complete(
        status: Int,
        data: ByteArray,
    ): MidiMessage =
        when (status and 0xF0) {
            0xB0 -> ControlChange(MidiChannel.fromWire(status and 0x0F), data[0].toInt(), data[1].toInt())
            0xC0 -> ProgramChange(MidiChannel.fromWire(status and 0x0F), data[0].toInt())
            else -> OtherMessage(byteArrayOf(status.toByte()) + data)
        }

    private fun dataLength(status: Int): Int =
        when (status and 0xF0) {
            0xC0, 0xD0 -> {
                1
            }

            0xF0 -> {
                when (status) {
                    0xF1, 0xF3 -> 1
                    0xF2 -> 2
                    else -> 0
                }
            }

            else -> {
                2
            }
        }
}
