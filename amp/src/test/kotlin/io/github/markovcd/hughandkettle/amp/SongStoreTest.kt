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
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SongStoreTest {
    private val folder: Path = Files.createTempDirectory("songs")
    private val store = SongStore(folder.resolve("songs"))
    private val amp = AmpModels.grandMeisterDeluxe40

    @AfterEach
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private fun list(): SongList = store.list(amp).getOrThrow()

    private fun names(): List<String> = list().songs.map { it.name }

    private fun song(
        name: String,
        tempo: Int = 120,
    ) = Song(amp, tempo, listOf(SongStep(5, 16)), name = name)

    private fun file(
        path: String,
        text: String,
    ) = folder
        .resolve("songs")
        .resolve(path)
        .also { it.parent.createDirectories() }
        .writeText(text)

    @Test
    fun `An amp with no songs lists none and has none open`() {
        assertEquals(SongList(emptyList()), list())
    }

    @Test
    fun `Songs saved read back in the order kept, for their amp alone`() {
        listOf("Highway Star", "Crunch rhythm", "Encore").forEach { store.save(song(it)).getOrThrow() }
        assertEquals(
            listOf(
                "Highway Star",
                "Crunch rhythm",
                "Encore",
            ),
            SongStore(folder.resolve("songs")).list(amp).getOrThrow().songs.map {
                it.name
            },
        )
        assertEquals(song("Encore"), list().find("encore")?.song)
        assertEquals(emptyList(), store.list(AmpModels.blackSpirit200).getOrThrow().songs)
    }

    @Test
    fun `A name is taken regardless of case, unless the save replaces it`() {
        store.save(song("Encore")).getOrThrow()
        assertTrue(store.save(song("ENCORE", 90)).isFailure)
        store.save(song("Encore", 90), replace = true).getOrThrow()
        assertEquals(listOf(song("Encore", 90)), list().songs.map { it.song })
    }

    @Test
    fun `A song renamed, moved and deleted keeps the rest in place`() {
        listOf("A", "B", "C").forEach { store.save(song(it)).getOrThrow() }
        store.rename(amp, "b", "Bee").getOrThrow()
        assertTrue(store.rename(amp, "A", "C").isFailure)
        store.move(amp, "C", 1).getOrThrow()
        assertEquals(listOf("C", "A", "Bee"), names())
        assertEquals("Bee", list().find("Bee")?.song?.name)
        store.delete(amp, "a").getOrThrow()
        assertEquals(listOf("C", "Bee"), names())
    }

    @Test
    fun `A tone renamed is renamed in every song of its amp that steps on it, and in no other`() {
        val lead = SongStep(beats = 8, tone = "Lead solo")
        store.save(Song(amp, 120, listOf(SongStep(5, 16), lead), name = "A")).getOrThrow()
        store.save(song("B")).getOrThrow()
        val written = folder.resolve("songs/${amp.id}/2.json").readText()
        store.renameTone(amp, "LEAD SOLO", "Solo").getOrThrow()
        assertEquals(listOf(SongStep(5, 16), lead.copy(tone = "Solo")), list().find("A")?.song?.steps)
        assertEquals(written, folder.resolve("songs/${amp.id}/2.json").readText())
    }

    @Test
    fun `A tone renamed that no song steps on writes nothing`() {
        store.renameTone(amp, "Lead solo", "Solo").getOrThrow()
        assertTrue(!folder.resolve("songs").toFile().exists())
    }

    @Test
    fun `The open song is remembered across a rename and forgotten with its song`() {
        listOf("A", "B").forEach { store.save(song(it)).getOrThrow() }
        store.open(amp, "B").getOrThrow()
        store.rename(amp, "B", "Bee").getOrThrow()
        assertEquals("Bee", list().open)
        store.delete(amp, "Bee").getOrThrow()
        assertNull(list().open)
    }

    @Test
    fun `A file that cannot be read is listed with its problem, never written over, and can be deleted`() {
        val newer = """{ "version": 2, "amp": "${amp.id}", "name": "Old demo", "tempo": 140, "steps": [] }"""
        file("${amp.id}/4.json", newer)
        file("${amp.id}/5.json", "{")
        val kept = list().songs
        assertEquals(listOf("Old demo", "Song 5"), kept.map { it.name })
        assertTrue(kept.all { it.song == null })
        assertTrue("4.json is version 2" in kept[0].problem.orEmpty())
        assertTrue(store.save(song("Old demo"), replace = true).isFailure)
        assertTrue(store.rename(amp, "Old demo", "New demo").isFailure)
        assertEquals(newer, folder.resolve("songs/${amp.id}/4.json").readText())
        store.delete(amp, "Old demo").getOrThrow()
        assertEquals(listOf("Song 5"), names())
    }

    @Test
    fun `A song file for another amp is listed as one that cannot be read`() {
        file("${amp.id}/1.json", SongFile.write(Song(AmpModels.blackSpirit200, name = "Stray")))
        assertTrue("is for amp 'black_spirit_200'" in list().find("Stray")?.problem.orEmpty())
    }

    @Test
    fun `The single song an older version kept is carried over once as Song 1, open, and left in place`() {
        val old =
            """{ "version": 1, "amp": "${amp.id}", "tempo": 140, "count_in": false, "steps": [ { "preset": 1, "beats": 8 } ] }"""
        file("${amp.id}.json", old)
        val first = list()
        assertEquals(
            SongList(
                listOf(KeptSong("Song 1", Song(amp, 140, listOf(SongStep(1, 8)), false, "Song 1"), null)),
                "Song 1",
                "Song 1",
            ),
            first,
        )
        assertNull(list().carried)
        store.delete(amp, "Song 1").getOrThrow()
        assertEquals(emptyList(), names())
        assertEquals(old, folder.resolve("songs/${amp.id}.json").readText())
    }

    @Test
    fun `An older single song that cannot be read is carried over as it is`() {
        file("${amp.id}.json", "{")
        val kept = list().songs.single()
        assertEquals("Song 1", kept.name)
        assertTrue("is not JSON" in kept.problem.orEmpty())
    }

    @Test
    fun `A name already here is set apart with the next number`() {
        listOf("Encore", "Encore 2").forEach { store.save(song(it)).getOrThrow() }
        assertEquals("Encore 3", list().apart("Encore"))
        assertEquals("Slow blues", list().apart("Slow blues"))
    }
}
