package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class SongStepTest {
    private fun longer(beats: Int) = SongStep(1, beats).longer().beats

    private fun shorter(beats: Int) = SongStep(1, beats).shorter().beats

    @Test
    fun `A step lengthens to the end of the next bar of four, however many beats it had`() {
        assertEquals(listOf(4, 4, 8, 8, 12, 999), listOf(1, 3, 4, 5, 9, 998).map(::longer))
    }

    @Test
    fun `A step shortens to the end of the bar before, and to one beat from a bar or less`() {
        assertEquals(listOf(1, 1, 1, 4, 4, 8), listOf(1, 3, 4, 5, 8, 9).map(::shorter))
    }
}
