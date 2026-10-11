package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class SectorsTest {
    @Test
    fun `Every sector of every count reads back as itself`() {
        val failures =
            (2..128).flatMap { count ->
                (0 until count).mapNotNull { index ->
                    val back = Sectors.index(Sectors.value(index, count), count)
                    if (back != index) "count $count sector $index came back as $back" else null
                }
            }
        assertEquals(emptyList(), failures.take(5), "${failures.size} failures")
    }

    @Test
    fun `The lowest and highest values fall in the first and last sector`() {
        assertEquals(0, Sectors.index(0, 4))
        assertEquals(3, Sectors.index(127, 4))
    }

    @Test
    fun `A four-way selector is sent from the middle of each 32-wide sector`() {
        assertEquals(listOf(15, 47, 79, 111), (0 until 4).map { Sectors.value(it, 4) })
    }
}
