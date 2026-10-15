package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiMessage
import kotlin.test.Test
import kotlin.test.assertEquals

class VolumeTrimTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val volume = amp.control("volume")!!
    private val gain = amp.control("gain")!!
    private val mute = amp.control("mute")!!
    private val quieter = VolumeTrim.of(volume, -20)

    /** The value block a recall of [preset] sends, its volume of 0..127 reported in 256 steps. */
    private fun recalled(
        preset: Int,
        volume: Int,
        channel: Int = 0,
    ) = ValueBlock(mapOf(57 to 2 * volume, 56 to 90), preset, channel).message()

    /** The value block a switch to [channel] sends. */
    private fun switched(
        channel: Int,
        volume: Int,
    ) = ValueBlock(mapOf(57 to 2 * volume), null, channel).message()

    private fun reported(volume: Int) = ControlReport(57, 2 * volume).message()

    /** What the trim sends for each of [messages] heard in turn, the amp reporting back each volume sent. */
    private fun VolumeTrim.answers(vararg messages: MidiMessage): List<List<Int>> {
        var trim = this
        return messages.map { message ->
            val sent = trim.restore(message)
            trim = trim.after(message)
            sent.filter { it.controller == volume.controller }.forEach { trim = trim.after(reported(it.value)) }
            sent.map { it.value }
        }
    }

    @Test
    fun `A recalled preset is answered with its own volume plus the trim`() {
        assertEquals(listOf(volume.set(40)), quieter.restore(recalled(3, 60)))
    }

    @Test
    fun `A trim of nothing sends nothing`() {
        assertEquals(emptyList(), VolumeTrim.of(volume).restore(recalled(3, 60)))
    }

    @Test
    fun `A trimmed volume stops at the ends of the knob`() {
        assertEquals(listOf(volume.set(0)), quieter.restore(recalled(3, 12)))
        assertEquals(listOf(volume.set(127)), VolumeTrim.of(volume, 20).restore(recalled(3, 120)))
    }

    @Test
    fun `Two channels keep the gap between their volumes`() {
        assertEquals(listOf(listOf(40), listOf(70)), quieter.answers(switched(0, 60), switched(1, 90)))
    }

    @Test
    fun `A channel switched back to is not trimmed twice`() {
        assertEquals(
            listOf(listOf(40), listOf(70), emptyList()),
            quieter.answers(switched(0, 60), switched(1, 90), switched(0, 40)),
        )
    }

    @Test
    fun `A volume turned by hand stays where it was turned`() {
        val trim = quieter.after(switched(0, 60)).after(reported(40)).after(reported(55))
        assertEquals(emptyList(), trim.after(switched(1, 90)).after(reported(70)).restore(switched(0, 55)))
    }

    @Test
    fun `A new trim moves the channel on now from its own volume`() {
        val trim = quieter.after(switched(0, 60)).after(reported(40))
        assertEquals(volume.set(50), trim.retrimmed(-10).second)
        assertEquals(volume.set(60), trim.retrimmed(0).second)
    }

    @Test
    fun `A new trim reaches a channel switched back to`() {
        val trim =
            quieter
                .after(switched(0, 60))
                .after(reported(40))
                .after(switched(1, 90))
                .after(reported(70))
        assertEquals(listOf(volume.set(50)), trim.retrimmed(-10).first.restore(switched(0, 40)))
    }

    @Test
    fun `A preset recalled again is answered on its program change, ahead of its block`() {
        val trim = quieter.after(recalled(3, 60)).after(reported(40))
        assertEquals(listOf(listOf(40), emptyList()), trim.answers(amp.recall(3), recalled(3, 40)))
    }

    @Test
    fun `A preset whose block still carries its own volume after the early answer is answered again`() {
        val trim = quieter.after(recalled(3, 60)).after(reported(40))
        assertEquals(listOf(listOf(40), listOf(40)), trim.answers(amp.recall(3), recalled(3, 60)))
    }

    @Test
    fun `A preset never heard is answered only on its block`() {
        assertEquals(listOf(emptyList(), listOf(40)), quieter.answers(amp.recall(3), recalled(3, 60)))
    }

    @Test
    fun `A recall from the app goes behind the mute, lifted after the block is answered`() {
        val (trim, messages) = quieter.recall(amp.recall(3), mute)
        assertEquals(listOf(mute.switched(true), amp.recall(3)), messages)
        assertEquals(listOf(volume.set(40), mute.switched(false)), trim.restore(recalled(3, 60)))
        assertEquals(
            emptyList(),
            trim.after(recalled(3, 60)).restore(recalled(4, 40)).filter {
                it.controller ==
                    mute.controller
            },
        )
    }

    @Test
    fun `A recall whose block never comes can have its mute lifted`() {
        val (trim, _) = quieter.recall(amp.recall(3), mute)
        assertEquals(mute.switched(false), trim.unmuted().second)
        assertEquals(
            null,
            trim
                .unmuted()
                .first
                .unmuted()
                .second,
        )
    }

    @Test
    fun `A volume the app sends is not overruled by the block of the channel it switched to`() {
        val trim = quieter.after(switched(0, 60)).after(reported(40)).sent(volume.set(77))
        assertEquals(listOf(volume.set(77)), trim.restore(switched(1, 90)))
    }

    @Test
    fun `A tone recalled under a trim plays its volume trimmed, and a tone saved keeps the amp's own`() {
        assertEquals(listOf(gain.set(9), volume.set(40)), quieter.trimmed(listOf(gain.set(9), volume.set(60))))
        assertEquals(mapOf("gain" to 9, "volume" to 60), quieter.untrimmed(mapOf("gain" to 9, "volume" to 40)))
    }
}
