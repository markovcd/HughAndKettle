package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.app.Cable
import io.github.markovcd.hughandkettle.app.CableLink
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.JavaCable
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import javax.sound.midi.MidiDevice
import javax.sound.midi.Receiver
import javax.sound.midi.ShortMessage
import javax.sound.midi.Transmitter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import javax.sound.midi.MidiMessage as JavaMidiMessage

class JavaCableJackTest {
    private val plugged = mutableListOf<JavaCable>()
    private val jack = JavaCableJack { Result.success(plugged.toList()) }

    @Test
    fun `A cable plugged in later is listed once the jack looks again, and keeps its id`() {
        jack.look()
        assertEquals(emptyList(), jack.cables.value)

        plugged += cable("UM-ONE")
        jack.look()
        plugged += cable("CH345")
        jack.look()

        assertEquals(listOf(Cable(1, "UM-ONE"), Cable(2, "CH345")), jack.cables.value)
    }

    @Test
    fun `A port that only listens is not a cable`() {
        plugged += JavaCable("Keyboard", "", toAmp = null, fromAmp = FakeDevice())
        jack.look()

        assertEquals(emptyList(), jack.cables.value)
    }

    @Test
    fun `A message sent on an open cable reaches the amp whole`() {
        val toAmp = FakeDevice()
        plugged += JavaCable("UM-ONE", "", toAmp, fromAmp = null)
        jack.look()

        opened().send(ProgramChange(MidiChannel(1), 5)).getOrThrow()

        assertEquals(listOf(listOf(0xC0, 5)), toAmp.heard)
    }

    @Test
    fun `What the amp sends on the cable arrives as messages`() {
        val fromAmp = FakeDevice()
        plugged += JavaCable("UM-ONE", "", FakeDevice(), fromAmp)
        jack.look()
        val heard = mutableListOf<MidiMessage>()

        opened(onMessage = { heard += it })
        fromAmp.say(ShortMessage(ShortMessage.CONTROL_CHANGE, 0, 56, 100))

        assertEquals(listOf<MidiMessage>(ControlChange(MidiChannel(1), 56, 100)), heard)
    }

    @Test
    fun `An open cable gone when the jack looks again is lost as unplugged, and closing releases it`() {
        val toAmp = FakeDevice()
        plugged += JavaCable("UM-ONE", "", toAmp, fromAmp = null)
        jack.look()
        var lost: String? = null
        val link = opened(onLost = { lost = it })
        assertTrue(toAmp.isOpen)

        plugged.clear()
        jack.look()
        link.close()

        assertEquals("unplugged", lost)
        assertFalse(toAmp.isOpen)
    }

    private fun opened(
        onMessage: (MidiMessage) -> Unit = {},
        onLost: (String) -> Unit = {},
    ): CableLink {
        var result: Result<CableLink>? = null
        jack.open(jack.cables.value.single(), onMessage, onLost) { result = it }
        return result!!.getOrThrow()
    }

    private fun cable(name: String) = JavaCable(name, "", FakeDevice(), FakeDevice())

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
