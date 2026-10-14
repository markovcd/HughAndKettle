package io.github.markovcd.hughandkettle.amp

import kotlin.test.Test
import kotlin.test.assertEquals

class ToneImportTest {
    @Test
    fun `A tone file reads as its one tone and a GrandMeister preset file as its presets`() {
        val tone = Tone("Rhythm", AmpModels.blackSpirit200, mapOf("gain" to 40))
        assertEquals(listOf(tone), ToneImport.read(ToneFile.write(tone).toByteArray()).getOrThrow().tones)
        val presets = ToneImportTest::class.java.getResource("/presets/test-bank.gm40memory")!!.readBytes()
        assertEquals(
            3,
            ToneImport
                .read(presets)
                .getOrThrow()
                .tones.size,
        )
    }
}
