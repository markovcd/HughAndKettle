package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The fixture is a preset file in the shape the GrandMeister Deluxe 40 Remote app writes, made with
 * Python's `plistlib`: "Clean Chorus", "Lead", a second "Lead", and an empty slot.
 */
class Gm40MemoryTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val bytes = Gm40MemoryTest::class.java.getResource("/presets/test-bank.gm40memory")!!.readBytes()
    private val tones = Gm40Memory.read(bytes).getOrThrow().tones

    private val clean = tones[0]
    private val lead = tones[1]
    private val secondLead = tones[2]

    /** The option a selector of [tone] is set to, by name. */
    private fun option(
        tone: Tone,
        id: String,
    ): String {
        val options = (amp.control(id)!!.kind as ControlKind.Selector).options
        return options[Sectors.index(tone.values.getValue(id), options.size)]
    }

    private fun why(bytes: ByteArray): String =
        Gm40Memory.read(bytes).exceptionOrNull()?.message ?: "read without failing"

    @Test
    fun `A preset file's named presets read as GrandMeister Deluxe 40 tones, the empty slots left out`() {
        assertEquals(listOf("Clean Chorus", "Lead", "Lead"), tones.map { it.name })
        assertTrue(tones.all { it.amp == amp })
    }

    @Test
    fun `A knob is scaled from the app's 0 to 100, and the delay time from milliseconds`() {
        assertEquals(127, clean.values["gain"])
        assertEquals(102, clean.values["volume"])
        assertEquals(amp.control("delay_time")!!.scale!!.value(430), clean.values["delay_time"])
        assertEquals(127, lead.values["delay_time"])
    }

    @Test
    fun `An effect is on exactly when its level is above 0`() {
        val effects = listOf("modulation", "reverb", "delay")
        assertEquals(listOf(127, 127, 0), effects.map { clean.values[it] })
        assertEquals(listOf(0, 0, 127), effects.map { lead.values[it] })
    }

    @Test
    fun `The channel, modulation type and power soak read as the options they name`() {
        assertEquals(
            listOf("Clean", "Phaser", "20 W"),
            listOf("channel", "modulation_type", "power_soak").map {
                option(clean, it)
            },
        )
        assertEquals(listOf("Lead", "Off"), listOf("channel", "power_soak").map { option(lead, it) })
        assertEquals(listOf("Ultra", "40 W"), listOf("channel", "power_soak").map { option(secondLead, it) })
    }

    @Test
    fun `The switches the app keeps read as on and off`() {
        assertEquals(
            listOf(0, 127, 0),
            listOf("boost", "noise_gate", "fx_loop").map { clean.values[it] },
        )
        assertEquals(listOf(127, 0, 0), listOf("boost", "noise_gate", "fx_loop").map { lead.values[it] })
        assertEquals(127, secondLead.values["fx_loop"])
    }

    @Test
    fun `A file that is not a preset file is refused with what it is not`() {
        assertTrue("not a binary property list" in why("{}".toByteArray()), why("{}".toByteArray()))
        assertTrue(Gm40Memory.read(bytes.copyOf(bytes.size - 40) + bytes.takeLast(32)).isFailure, "a cut file")
        assertTrue(
            "has no named presets" in
                why(ToneFileTest::class.java.getResource("/presets/empty-bank.gm40memory")!!.readBytes()),
        )
    }

    @Test
    fun `A value the app would not write is left out and said, and the rest of the preset kept`() {
        val odd = Gm40MemoryTest::class.java.getResource("/presets/odd-values.gm40memory")!!.readBytes()
        val read = Gm40Memory.read(odd).getOrThrow()
        val values = read.tones.single().values
        assertEquals(listOf(null, null, null), listOf("channel", "gain", "delay_time").map { values[it] })
        assertEquals(64, values["bass"])
        assertEquals(3, read.skipped.size, "${read.skipped}")
    }

    @Test
    fun `A damaged preset file is reported, never thrown`() {
        val thrown =
            (bytes.indices).flatMap { at ->
                listOf(0x00, 0xFF, 0x0F, 0x80).mapNotNull { byte ->
                    val damaged = bytes.copyOf().also { it[at] = byte.toByte() }
                    runCatching { Gm40Memory.read(damaged) }.exceptionOrNull()?.let { "byte $at as $byte: $it" }
                }
            } +
                (0 until bytes.size).mapNotNull { size ->
                    runCatching {
                        Gm40Memory.read(
                            bytes.copyOf(size),
                        )
                    }.exceptionOrNull()?.let { "first $size bytes: $it" }
                }
        assertEquals(emptyList(), thrown.take(5))
    }
}
