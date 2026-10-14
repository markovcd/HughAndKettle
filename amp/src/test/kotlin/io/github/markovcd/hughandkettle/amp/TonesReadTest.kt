package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class TonesReadTest {
    private val amp = AmpModels.grandMeisterDeluxe40

    private fun named(vararg names: String) = TonesRead(names.map { Tone(it, amp, emptyMap()) }, emptyList())

    @Test
    fun `A name taken already or earlier in the list gets the next free number, regardless of case`() {
        val apart = named("Clean", "clean", "Lead", "Clean 2").namedApartFrom(listOf("LEAD"))
        assertEquals(listOf("Clean", "clean 2", "Lead 2", "Clean 2 2"), apart.tones.map { it.name })
    }
}
