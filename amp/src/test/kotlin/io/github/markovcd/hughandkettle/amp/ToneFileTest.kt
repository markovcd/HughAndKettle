package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneFileTest {
    private val amp = AmpModels.blackSpirit200

    private fun read(text: String): Result<ToneRead> = ToneFile.read(text)

    private fun why(text: String): String = read(text).exceptionOrNull()?.message ?: "read without failing"

    @Test
    fun `Every control of every amp reads back as the value it was written with`() {
        val wrong =
            AmpModels.all.flatMap { model ->
                val tone = Tone("All", model, model.controls.mapIndexed { i, control -> control.id to i % 128 }.toMap())
                val back = read(ToneFile.write(tone)).getOrThrow()
                if (back == ToneRead(tone, emptyList())) emptyList() else listOf(model.id)
            }
        assertEquals(emptyList(), wrong)
    }

    @Test
    fun `A version 1 file reads`() {
        val text = ToneFileTest::class.java.getResource("/tones/version-1.json")!!.readText()
        val read = read(text).getOrThrow()
        assertEquals("Crunch rhythm", read.tone.name)
        assertEquals(amp, read.tone.amp)
        assertEquals(72, read.tone.values["gain"])
        assertEquals(9, read.tone.values.size)
        assertEquals(emptyList(), read.skipped)
    }

    @Test
    fun `A file from a newer version is refused whole and says so`() {
        val text = """{"version": 2, "amp": "black_spirit_200", "name": "x", "values": {"gain": 1}}"""
        assertTrue("version 2, from a newer Hugh & Kettle" in why(text), why(text))
    }

    @Test
    fun `A control the amp does not have is skipped and reported`() {
        val text = """{"version": 1, "amp": "black_spirit_200", "name": "x", "values": {"gain": 1, "power_soak": 2}}"""
        val read = read(text).getOrThrow()
        assertEquals(mapOf("gain" to 1), read.tone.values)
        assertEquals(listOf("Black Spirit 200 has no control 'power_soak'"), read.skipped)
    }

    @Test
    fun `A value outside 0 to 127 or not a number is skipped and reported`() {
        val values = """{"gain": 300, "mid": "loud", "bass": 5}"""
        val text = """{"version": 1, "amp": "black_spirit_200", "name": "x", "values": $values}"""
        val read = read(text).getOrThrow()
        assertEquals(mapOf("bass" to 5), read.tone.values)
        assertEquals(listOf("'gain' is 300, not 0..127", "'mid' is \"loud\", not 0..127"), read.skipped)
    }

    @Test
    fun `A file that is not a tone says what it lacks`() {
        val cases =
            mapOf(
                "not json" to "is not JSON",
                "[1]" to "is not a JSON object",
                """{"amp": "black_spirit_200"}""" to "has no version",
                """{"version": "1"}""" to "has no version",
                """{"version": 0}""" to "versions start at 1",
                """{"version": 1, "name": "x", "values": {}}""" to "names no amp",
                """{"version": 1, "amp": "jcm800", "name": "x", "values": {}}""" to "amp 'jcm800'",
                """{"version": 1, "amp": "black_spirit_200", "name": " ", "values": {}}""" to "has no name",
                """{"version": 1, "amp": "black_spirit_200", "name": "x"}""" to "has no values",
            )
        val wrong =
            cases
                .filter { (text, expected) ->
                    expected !in why(text)
                }.map { (text, _) -> "$text: ${why(text)}" }
        assertEquals(emptyList(), wrong.take(3))
    }
}
