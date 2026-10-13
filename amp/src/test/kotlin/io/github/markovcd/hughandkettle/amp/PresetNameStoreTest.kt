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

class PresetNameStoreTest {
    private val folder: Path = Files.createTempDirectory("preset-names")
    private val store = PresetNameStore(folder.resolve("names"))
    private val amp = AmpModels.grandMeisterDeluxe40

    @AfterEach
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private fun file(text: String) =
        folder
            .resolve("names")
            .createDirectories()
            .resolve("${amp.id}.json")
            .writeText(text)

    @Test
    fun `An amp with no file has no preset names`() {
        assertEquals(PresetNames(amp), store.names(amp).getOrThrow())
    }

    @Test
    fun `Names given read back for their amp alone and a blank name takes one away`() {
        store.name(amp, 12, "Lead solo").getOrThrow()
        store.name(amp, 3, "Clean").getOrThrow()
        store.name(amp, 3, " ").getOrThrow()
        assertEquals(mapOf(12 to "Lead solo"), PresetNameStore(folder.resolve("names")).names(amp).getOrThrow().names)
        assertEquals(PresetNames(AmpModels.blackSpirit200), store.names(AmpModels.blackSpirit200).getOrThrow())
    }

    @Test
    fun `A version 1 file reads back`() {
        file("""{ "version": 1, "amp": "${amp.id}", "names": { "1": "Rhythm", "128": "Solo" } }""")
        assertEquals(mapOf(1 to "Rhythm", 128 to "Solo"), store.names(amp).getOrThrow().names)
    }

    @Test
    fun `A damaged or newer file is refused with its path and never written over`() {
        val cases =
            listOf(
                "not json",
                """{ "version": 2, "amp": "${amp.id}", "names": {} }""",
                """{ "version": 1, "amp": "${amp.id}", "names": { "129": "Lost" } }""",
                """{ "version": 1, "amp": "${amp.id}", "names": { "3": "" } }""",
            )
        for (text in cases) {
            file(text)
            val read = store.names(amp)
            assertTrue(
                read
                    .exceptionOrNull()
                    ?.message
                    .orEmpty()
                    .contains("${amp.id}.json"),
                "$text: $read",
            )
            assertTrue(store.name(amp, 1, "Rhythm").isFailure, text)
            assertEquals(text, folder.resolve("names/${amp.id}.json").readText())
        }
    }

    @Test
    fun `A preset the amp does not have is refused`() {
        assertTrue(store.name(amp, 0, "Lost").isFailure)
        assertTrue(store.name(amp, amp.presetCount + 1, "Lost").isFailure)
    }
}
