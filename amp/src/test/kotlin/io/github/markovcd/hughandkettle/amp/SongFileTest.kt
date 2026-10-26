package io.github.markovcd.hughandkettle.amp

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongFileTest {
    private val amp = AmpModels.grandMeisterDeluxe40

    private fun failure(text: String): String =
        SongFile.read(text).exceptionOrNull()?.message ?: "read without a failure"

    @Test
    fun `A song written reads back as the same song`() {
        val song = Song(amp, 96, listOf(SongStep(5, 32), SongStep(12, 16)), countIn = false, name = "Highway Star")
        assertEquals(song, SongFile.read(SongFile.write(song)).getOrThrow())
    }

    @Test
    fun `A song of presets and tones reads back as itself`() {
        val song = Song(amp, 120, listOf(SongStep(5, 32), SongStep(beats = 16, tone = "Lead solo")), name = "Mixed")
        assertEquals(song, SongFile.read(SongFile.write(song)).getOrThrow())
        val written =
            Json
                .parseToJsonElement(SongFile.write(song))
                .jsonObject["steps"]
                ?.jsonArray
                ?.get(1)
        assertEquals(Json.parseToJsonElement("""{ "tone": "Lead solo", "beats": 16 }"""), written)
    }

    @Test
    fun `A step's note length reads back, and a note this version does not know leaves the step without one`() {
        val dotted = NoteLength.of("1/8d")
        val song =
            Song(amp, 120, listOf(SongStep(5, 8, note = dotted), SongStep(beats = 4, tone = "Lead", note = dotted)))
        assertEquals(song, SongFile.read(SongFile.write(song)).getOrThrow())
        assertEquals(
            Song(amp, 120, listOf(SongStep(5, 8))),
            SongFile
                .read(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "preset": 5, "beats": 8, "note": "1/32" } ] }""",
                ).getOrThrow(),
        )
    }

    @Test
    fun `A version 1 file with no name or count-in is untitled and counts in`() {
        assertEquals(
            Song(amp, 140, listOf(SongStep(1, 8), SongStep(128, 4))),
            SongFile
                .read(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 140, "steps": [ { "preset": 1, "beats": 8 }, { "preset": 128, "beats": 4 } ] }""",
                ).getOrThrow(),
        )
    }

    @Test
    fun `A damaged file says what is wrong and where`() {
        assertTrue("is not JSON" in failure("{"))
        assertTrue(
            "is version 2, from a newer Hugh & Kettle" in
                failure("""{ "version": 2, "amp": "${amp.id}", "tempo": 140, "steps": [] }"""),
        )
        assertTrue("has tempo 900" in failure("""{ "version": 1, "amp": "${amp.id}", "tempo": 900, "steps": [] }"""))
        assertTrue(
            "has count_in \"yes\"" in
                failure("""{ "version": 1, "amp": "${amp.id}", "tempo": 140, "count_in": "yes", "steps": [] }"""),
        )
        assertTrue(
            "has name 3" in failure("""{ "version": 1, "amp": "${amp.id}", "name": 3, "tempo": 140, "steps": [] }"""),
        )
        assertTrue(
            "step 2 has preset 129" in
                failure(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "preset": 1, "beats": 8 }, { "preset": 129, "beats": 8 } ] }""",
                ),
        )
        assertTrue(
            "step 1 has tone 3; it is a name" in
                failure(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "tone": 3, "beats": 8 } ] }""",
                ),
        )
        assertTrue(
            "step 1 lasts 0 beats" in
                failure(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "preset": 1, "beats": 0 } ] }""",
                ),
        )
        assertTrue(
            "is for amp 'fireball_100', which" in
                failure("""{ "version": 1, "amp": "fireball_100", "tempo": 120, "steps": [] }"""),
        )
    }

    @Test
    fun `The amp and name of a file are read even when the rest of it cannot be`() {
        val newer = """{ "version": 7, "amp": "fireball_100", "name": "Old demo", "tempo": "fast" }"""
        assertEquals("fireball_100", SongFile.ampOf(newer))
        assertEquals("Old demo", SongFile.nameOf(newer))
        assertNull(SongFile.ampOf("{"))
    }
}
