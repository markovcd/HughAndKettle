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

class SongStoreTest {
    private val folder: Path = Files.createTempDirectory("songs")
    private val store = SongStore(folder.resolve("songs"))
    private val amp = AmpModels.grandMeisterDeluxe40

    @AfterEach
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private fun file(text: String) =
        folder
            .resolve("songs")
            .createDirectories()
            .resolve("${amp.id}.json")
            .writeText(text)

    private fun failure(text: String): String {
        file(text)
        return store.song(amp).exceptionOrNull()?.message ?: "read without a failure"
    }

    @Test
    fun `An amp with no file has a song with no steps`() {
        assertEquals(Song(amp), store.song(amp).getOrThrow())
    }

    @Test
    fun `A song saved reads back for its amp alone`() {
        val song = Song(amp, 96, listOf(SongStep(5, 32), SongStep(12, 16)))
        store.save(song).getOrThrow()
        assertEquals(song, SongStore(folder.resolve("songs")).song(amp).getOrThrow())
        assertEquals(Song(AmpModels.blackSpirit200), store.song(AmpModels.blackSpirit200).getOrThrow())
    }

    @Test
    fun `A version 1 file reads back`() {
        file(
            """{ "version": 1, "amp": "${amp.id}", "tempo": 140, "steps": [ { "preset": 1, "beats": 8 }, { "preset": 128, "beats": 4 } ] }""",
        )
        assertEquals(Song(amp, 140, listOf(SongStep(1, 8), SongStep(128, 4))), store.song(amp).getOrThrow())
    }

    @Test
    fun `A song kept without its count-in reads back without one`() {
        store.save(Song(amp, 96, listOf(SongStep(5, 32)), countIn = false)).getOrThrow()
        assertEquals(false, store.song(amp).getOrThrow().countIn)
        assertTrue(
            "has count_in \"yes\"" in
                failure("""{ "version": 1, "amp": "${amp.id}", "tempo": 140, "count_in": "yes", "steps": [] }"""),
        )
    }

    @Test
    fun `A file from a newer version is refused and never written over`() {
        val newer = """{ "version": 2, "amp": "${amp.id}", "tempo": 140, "steps": [] }"""
        assertTrue("is version 2, from a newer Hugh & Kettle" in failure(newer))
        assertTrue(store.save(Song(amp, 100)).isFailure)
        assertEquals(newer, folder.resolve("songs/${amp.id}.json").readText())
    }

    @Test
    fun `A damaged file says what is wrong and where`() {
        assertTrue("is not JSON" in failure("{"))
        assertTrue("has tempo 900" in failure("""{ "version": 1, "amp": "${amp.id}", "tempo": 900, "steps": [] }"""))
        assertTrue(
            "step 2 has preset 129" in
                failure(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "preset": 1, "beats": 8 }, { "preset": 129, "beats": 8 } ] }""",
                ),
        )
        assertTrue(
            "step 1 lasts 0 beats" in
                failure(
                    """{ "version": 1, "amp": "${amp.id}", "tempo": 120, "steps": [ { "preset": 1, "beats": 0 } ] }""",
                ),
        )
        assertTrue(
            "is for amp 'black_spirit_200'" in
                failure("""{ "version": 1, "amp": "black_spirit_200", "tempo": 120, "steps": [] }"""),
        )
    }
}
