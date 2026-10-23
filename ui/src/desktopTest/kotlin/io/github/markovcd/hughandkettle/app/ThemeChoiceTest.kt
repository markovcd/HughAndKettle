package io.github.markovcd.hughandkettle.app

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeChoiceTest {
    @Test
    fun `Every choice reads back as itself`() {
        assertEquals(ThemeChoice.entries.toList(), ThemeChoice.entries.map { ThemeChoice.read(it.write()) })
    }

    @Test
    fun `Nothing saved, a damaged value or a newer version reads as dark`() {
        val texts = listOf(null, "", "light", "1 sepia", "2 light", "x light")
        assertEquals(texts.map { ThemeChoice.Dark }, texts.map(ThemeChoice::read))
    }

    @Test
    fun `Dark is dark, light is light, and the system's choice follows the system`() {
        val worn = { system: Boolean -> ThemeChoice.entries.associateWith { it.dark(system) } }
        assertEquals(
            mapOf(ThemeChoice.Dark to true, ThemeChoice.Light to false, ThemeChoice.System to true),
            worn(true),
        )
        assertEquals(
            mapOf(ThemeChoice.Dark to true, ThemeChoice.Light to false, ThemeChoice.System to false),
            worn(false),
        )
    }
}
