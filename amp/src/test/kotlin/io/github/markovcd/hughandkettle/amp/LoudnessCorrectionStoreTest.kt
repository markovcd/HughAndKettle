package io.github.markovcd.hughandkettle.amp

import org.junit.jupiter.api.AfterEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoudnessCorrectionStoreTest {
    private val folder: Path = Files.createTempDirectory("loudness")
    private val store = LoudnessCorrectionStore(folder.resolve("loudness"))
    private val amp = AmpModels.grandMeisterDeluxe40

    @AfterEach
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private fun file(text: String) =
        folder
            .resolve("loudness")
            .createDirectories()
            .resolve("${amp.id}.json")
            .writeText(text)

    @Test
    fun `An amp with no file has no corrections`() {
        assertEquals(LoudnessCorrections(amp), store.corrections(amp).getOrThrow())
    }

    @Test
    fun `Corrections kept read back for their amp`() {
        store.keep(LoudnessCorrections(amp, mapOf(12 to -11, 7 to 4))).getOrThrow()
        val read = LoudnessCorrectionStore(folder.resolve("loudness")).corrections(amp).getOrThrow()
        assertEquals(mapOf(7 to 4, 12 to -11), read.amounts)
    }

    @Test
    fun `A version 1 file reads`() {
        file("""{ "version": 1, "amp": "${amp.id}", "corrections": { "12": -11 } }""")
        assertEquals(mapOf(12 to -11), store.corrections(amp).getOrThrow().amounts)
    }

    @Test
    fun `A file from a newer version is refused and never written over`() {
        file("""{ "version": 2, "amp": "${amp.id}", "corrections": {} }""")
        assertTrue(store.corrections(amp).isFailure)
        assertTrue(store.keep(LoudnessCorrections(amp, mapOf(1 to 3))).isFailure)
        assertTrue(folder.resolve("loudness/${amp.id}.json").readText().contains("\"version\": 2"))
    }

    @Test
    fun `A correction out of range is refused with the preset it names`() {
        file("""{ "version": 1, "amp": "${amp.id}", "corrections": { "12": 300 } }""")
        val failure = store.corrections(amp).exceptionOrNull()!!
        assertTrue(failure.message!!.contains("preset 12"), failure.message)
    }

    @Test
    fun `New corrections replace the old for the same presets, and 0 takes one away`() {
        val kept = LoudnessCorrections(amp, mapOf(12 to -11, 7 to 4)).with(mapOf(7 to 0, 9 to 9))
        assertEquals(mapOf(12 to -11, 9 to 9), kept.amounts)
    }
}
