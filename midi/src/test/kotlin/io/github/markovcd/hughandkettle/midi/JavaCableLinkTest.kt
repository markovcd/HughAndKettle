package io.github.markovcd.hughandkettle.midi

import javax.sound.midi.MidiDevice
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.Transmitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import javax.sound.midi.MidiMessage as JavaMidiMessage

class JavaCableLinkTest {
    @Test
    fun `A message sent reaches the amp whole`() {
        val toAmp = FakeDevice()
        val link = JavaCableLink.open(JavaCable("UM-ONE", "", toAmp, null)).getOrThrow()

        link.send(ControlChange(MidiChannel(1), 56, 100)).getOrThrow()

        assertEquals(listOf(listOf(0xB0, 56, 100)), toAmp.heard)
    }

    @Test
    fun `Bytes holding two messages go out as two`() {
        val toAmp = FakeDevice()
        val link = JavaCableLink.open(JavaCable("UM-ONE", "", toAmp, null)).getOrThrow()

        link.write(byteArrayOf(0xC0.toByte(), 5, 0xB0.toByte(), 7, 64)).getOrThrow()

        assertEquals(listOf(listOf(0xC0, 5), listOf(0xB0, 7, 64)), toAmp.heard)
    }

    @Test
    fun `What the amp sends arrives as messages`() {
        val fromAmp = FakeDevice()
        val heard = mutableListOf<MidiMessage>()
        JavaCableLink.open(JavaCable("UM-ONE", "", FakeDevice(), fromAmp)) { heard += it }.getOrThrow()

        fromAmp.say(ShortMessage(ShortMessage.PROGRAM_CHANGE, 0, 9, 0))

        assertEquals(listOf<MidiMessage>(ProgramChange(MidiChannel(1), 9)), heard)
    }

    @Test
    fun `A cable that only listens refuses a send with its name, and closing releases both ports`() {
        val fromAmp = FakeDevice()
        val link = JavaCableLink.open(JavaCable("Keyboard", "", null, fromAmp)).getOrThrow()

        val sent = link.send(ProgramChange(MidiChannel(1), 1))
        link.close()

        assertEquals("Keyboard takes no messages to send", sent.exceptionOrNull()?.message)
        assertFalse(fromAmp.isOpen)
    }

    @Test
    fun `A cable with no port at all is a failure, not a throw`() {
        assertTrue(JavaCableLink.open(JavaCable("Ghost", "", null, null)).isFailure)
    }

    /** A MIDI port that keeps what it is sent and says what a test tells it to. */
    private class FakeDevice : MidiDevice {
        val heard = mutableListOf<List<Int>>()
        private var open = false
        private val transmitter =
            object : Transmitter {
                private var receiver: Receiver? = null

                override fun setReceiver(receiver: Receiver?) {
                    this.receiver = receiver
                }

                override fun getReceiver() = receiver

                override fun close() = Unit
            }

        fun say(message: JavaMidiMessage) = transmitter.receiver!!.send(message, -1)

        override fun getDeviceInfo(): MidiDevice.Info = object : MidiDevice.Info("fake", "", "", "") {}

        override fun open() {
            open = true
        }

        override fun close() {
            open = false
        }

        override fun isOpen() = open

        override fun getMicrosecondPosition() = -1L

        override fun getMaxReceivers() = -1

        override fun getMaxTransmitters() = -1

        override fun getReceiver(): Receiver =
            object : Receiver {
                override fun send(
                    message: JavaMidiMessage,
                    timeStamp: Long,
                ) {
                    heard += message.message.map { it.toInt() and 0xFF }
                }

                override fun close() = Unit
            }

        override fun getReceivers() = emptyList<Receiver>()

        override fun getTransmitter() = transmitter

        override fun getTransmitters() = listOf(transmitter)
    }
}
