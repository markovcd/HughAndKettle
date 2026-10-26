package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.amp.PresetWriting.Stage
import io.github.markovcd.hughandkettle.amp.PresetWriting.Why
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PresetWritingTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val tone = Tone("Lead solo", amp, mapOf("gain" to 100, "volume" to 90, "delay" to 0, "delay_volume" to 80))

    private val recall47 = ProgramChange(MidiChannel.Default, 46)

    /** The block the amp sends for [preset] holding [values], keyed by controller and 0..255 for knobs. */
    private fun block(
        preset: Int?,
        values: Map<Int, Int>,
    ) = ValueBlock(values, preset).message()

    private val stored = ControlReport(80, 127).message()

    /** The write after the amp says each of [messages] in turn, and everything it sent along the way. */
    private fun heard(
        writing: PresetWriting,
        vararg messages: MidiMessage,
    ): Pair<PresetWriting, List<MidiMessage>> =
        messages.fold(writing to emptyList()) { (now, sent), message ->
            val (next, more) = now.after(message)
            next to sent + more
        }

    private fun waiting(): PresetWriting {
        val (writing, _) = PresetWriting.start(tone, 47)
        return heard(writing, block(47, mapOf(56 to 60, 57 to 120))).first
    }

    @Test
    fun `A write starts by recalling the preset`() {
        val (writing, sent) = PresetWriting.start(tone, 47)
        assertEquals(listOf<MidiMessage>(recall47), sent)
        assertEquals(Stage.BackingUp, writing.stage)
        assertEquals(PresetWriting.answerLimit, writing.limit)
    }

    @Test
    fun `The preset's block is kept as the backup and the tone is sent`() {
        val (start, _) = PresetWriting.start(tone, 47)
        val (writing, sent) = heard(start, block(null, mapOf(56 to 10)), block(47, mapOf(56 to 60, 57 to 120)))
        assertEquals(Stage.Waiting, writing.stage)
        assertEquals("Backup of Bank 12 · 3", writing.backup?.name)
        assertEquals(30 to 60, writing.backup?.values?.let { it["gain"] to it["volume"] })
        assertEquals(tone.recall(), sent)
        assertEquals(PresetWriting.storeLimit, writing.limit)
    }

    @Test
    fun `STORE pressed on the amp is answered with the preset's program change`() {
        val (writing, sent) = heard(waiting(), ControlChange(MidiChannel.Default, 56, 100), stored)
        assertEquals(Stage.Storing, writing.stage)
        assertEquals(listOf<MidiMessage>(recall47), sent)
    }

    @Test
    fun `The stored preset's block matching the tone ends the write stored`() {
        val values = mapOf(56 to 200, 57 to 181, 28 to 0)
        val (writing, _) = heard(waiting(), stored, ControlReport(80, 0).message(), block(47, values))
        assertEquals(Stage.Stored(emptyList()), writing.stage)
        assertTrue(writing.over)
    }

    @Test
    fun `A value stored otherwise is a difference, a level stored at 0 for an effect the tone has off is not`() {
        val (writing, _) = heard(waiting(), stored, block(47, mapOf(56 to 200, 57 to 168, 28 to 0)))
        val differences = (writing.stage as Stage.Stored).differences
        assertEquals(listOf(Triple("volume", 84, 90)), differences.map { Triple(it.control.id, it.stored, it.tone) })
    }

    @Test
    fun `A block for another preset or after a channel switch does not end a step`() {
        val (backingUp, _) = heard(PresetWriting.start(tone, 47).first, block(46, mapOf()), block(null, mapOf()))
        assertEquals(Stage.BackingUp, backingUp.stage)
        val (storing, _) = heard(waiting(), stored, block(null, mapOf()))
        assertEquals(Stage.Storing, storing.stage)
    }

    @Test
    fun `STORE pressed before the backup came leaves it unknown what the preset holds`() {
        val (writing, sent) = heard(PresetWriting.start(tone, 47).first, stored, block(47, mapOf()))
        assertEquals(Stage.Unsure, writing.stage)
        assertEquals(emptyList(), sent)
    }

    @Test
    fun `Giving up writes nothing until STORE was pressed, and after it leaves it unknown`() {
        val (start, _) = PresetWriting.start(tone, 47)
        assertEquals(Stage.NotWritten(Why.NoBackup), start.gaveUp().stage)
        assertEquals(Stage.NotWritten(Why.NoStore), waiting().gaveUp().stage)
        assertEquals(Stage.NotWritten(Why.Cancelled), waiting().cancel().stage)
        assertEquals(Stage.NotWritten(Why.LinkLost), waiting().lost().stage)
        val storing = heard(waiting(), stored).first
        assertEquals(Stage.Unsure, storing.gaveUp().stage)
        assertEquals(Stage.Unsure, storing.cancel().stage)
    }

    @Test
    fun `A write left unknown is checked by recalling the preset and comparing its block`() {
        val unsure = heard(waiting(), stored).first.gaveUp()
        val (checking, sent) = unsure.check()
        assertEquals(listOf<MidiMessage>(recall47), sent)
        val (writing, _) = heard(checking, block(47, mapOf(56 to 200, 57 to 181)))
        assertEquals(Stage.Stored(emptyList()), writing.stage)
    }
}
