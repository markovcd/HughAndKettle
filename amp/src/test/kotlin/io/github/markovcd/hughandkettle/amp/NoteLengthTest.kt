package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NoteLengthTest {
    private fun note(id: String) = NoteLength.of(id)!!

    @Test
    fun `Each note lasts its share of a beat at a tempo`() {
        val at120 = NoteLength.ALL.associate { it.id to it.ms(120) }
        assertEquals(
            mapOf(
                "1/2" to 1000,
                "1/4" to 500,
                "1/8" to 250,
                "1/16" to 125,
                "1/4d" to 750,
                "1/8d" to 375,
                "1/4t" to 333,
                "1/8t" to 167,
            ),
            at120,
        )
    }

    @Test
    fun `A delay time reads back as the tempo at which the note lasts that long`() {
        assertEquals(126, note("1/8d").tempo(357))
        assertEquals(120, note("1/4").tempo(500))
    }

    @Test
    fun `A note is found by its id, and an unknown id is none`() {
        assertEquals(0.75, note("1/8d").beats)
        assertNull(NoteLength.of("1/32"))
    }
}
