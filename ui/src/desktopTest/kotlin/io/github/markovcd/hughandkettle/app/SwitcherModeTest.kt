package io.github.markovcd.hughandkettle.app

import kotlin.test.Test
import kotlin.test.assertEquals

class SwitcherModeTest {
    @Test
    fun `Every mode reads back as itself`() {
        assertEquals(SwitcherMode.entries.toList(), SwitcherMode.entries.map { SwitcherMode.read(it.write()) })
    }

    @Test
    fun `Nothing saved, a damaged value or a newer version reads as presets`() {
        val texts = listOf(null, "", "tones", "1 songs", "2 tones", "x tones")
        assertEquals(texts.map { SwitcherMode.Presets }, texts.map(SwitcherMode::read))
    }
}
