package io.github.markovcd.hughandkettle.amp

import org.junit.jupiter.api.AfterEach
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.deleteRecursively
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ToneStoreTest {
    private val folder: Path = Files.createTempDirectory("tones")
    private val store = ToneStore(folder.resolve("tones"))
    private val amp = AmpModels.blackSpirit200

    @AfterEach
    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    fun teardown() = folder.deleteRecursively()

    private fun tone(
        name: String,
        gain: Int = 1,
    ) = Tone(name, amp, mapOf("gain" to gain))

    private fun names(): List<String> = store.list().tones.map { it.name }

    private fun saved(vararg names: String) = names.forEach { store.save(tone(it)).getOrThrow() }

    @Test
    fun `A folder that does not exist holds no tones`() {
        assertEquals(ToneList(emptyList(), emptyList()), store.list())
    }

    @Test
    fun `Saved tones list in the order they were saved and read back whole`() {
        store.save(tone("Rhythm", 40)).getOrThrow()
        store.save(tone("Lead", 90)).getOrThrow()
        assertEquals(listOf(tone("Rhythm", 40), tone("Lead", 90)), store.list().tones)
        assertEquals(tone("Lead", 90), ToneStore(folder.resolve("tones")).find("lead"))
    }

    @Test
    fun `A second tone of the same name is refused unless it replaces the first in place`() {
        saved("Rhythm", "Lead")
        assertTrue(store.save(tone("rhythm", 7)).isFailure)
        store.save(tone("Rhythm", 7), replace = true).getOrThrow()
        assertEquals(listOf(tone("Rhythm", 7), tone("Lead")), store.list().tones)
    }

    @Test
    fun `Tones saved together are refused together when one name is taken`() {
        saved("Rhythm")
        assertTrue(store.save(listOf(tone("Verse"), tone("RHYTHM", 7))).isFailure)
        assertEquals(listOf("Rhythm"), names())
        store.save(listOf(tone("Verse"), tone("RHYTHM", 7), tone("Lead")), replace = true).getOrThrow()
        assertEquals(listOf(tone("RHYTHM", 7), tone("Verse"), tone("Lead")), store.list().tones)
    }

    @Test
    fun `A renamed tone keeps its place and its values`() {
        saved("Rhythm", "Lead")
        store.rename("rhythm", "Verse").getOrThrow()
        assertEquals(listOf("Verse", "Lead"), names())
    }

    @Test
    fun `A rename onto another tone's name or to nothing is refused`() {
        saved("Rhythm", "Lead")
        assertTrue(store.rename("Rhythm", "LEAD").isFailure)
        assertTrue(store.rename("Rhythm", " ").isFailure)
        assertTrue(store.rename("Solo", "Verse").isFailure)
        store.rename("Rhythm", "RHYTHM").getOrThrow()
        assertEquals(listOf("RHYTHM", "Lead"), names())
    }

    @Test
    fun `A deleted tone leaves the list and its file goes`() {
        saved("Rhythm", "Lead")
        store.delete("Rhythm").getOrThrow()
        assertEquals(listOf("Lead"), names())
        assertNull(store.find("Rhythm"))
        assertEquals(listOf("2.json"), folder.resolve("tones").listDirectoryEntries("*.json").map { it.name })
    }

    @Test
    fun `A moved tone takes the position it is given and the rest close up`() {
        saved("A", "B", "C", "D")
        store.move("D", 2).getOrThrow()
        assertEquals(listOf("A", "D", "B", "C"), names())
        store.move("a", 4).getOrThrow()
        assertEquals(listOf("D", "B", "C", "A"), names())
        assertTrue(store.move("A", 5).isFailure)
        assertTrue(store.move("A", 0).isFailure)
    }

    @Test
    fun `A file that does not read is reported, kept, and its name is not reused`() {
        saved("Rhythm")
        folder.resolve("tones/2.json").writeText("{ damaged")
        store.save(tone("Lead")).getOrThrow()
        val list = store.list()
        assertEquals(listOf("Rhythm", "Lead"), list.tones.map { it.name })
        assertEquals(1, list.problems.size)
        assertTrue(list.problems.single().startsWith("2.json: the tone file is not JSON"), list.problems.single())
        assertEquals("{ damaged", folder.resolve("tones/2.json").readText())
    }

    @Test
    fun `Without a readable order file the tones fall back to name order`() {
        saved("b", "C", "a")
        folder.resolve("tones/order").writeText("99\n3.json\n")
        assertEquals(listOf("a", "b", "C"), names())
        folder.resolve("tones/order").writeText("1\n3.json\n")
        assertEquals(listOf("a", "b", "C"), names())
        folder.resolve("tones/order").writeText("1\n2.json\n")
        assertEquals(listOf("C", "a", "b"), names())
    }

    @Test
    fun `A tone file dropped into the folder by hand is listed after the ordered ones`() {
        saved("Rhythm")
        folder.resolve("tones/by-hand.json").writeText(ToneFile.write(tone("Acoustic")))
        assertEquals(listOf("Rhythm", "Acoustic"), names())
    }

    @Test
    fun `A folder that cannot be written is a failure, not a throw`() {
        folder.resolve("blocked").writeText("a file where the folder should be")
        val result = ToneStore(folder.resolve("blocked")).save(tone("Rhythm"))
        assertTrue(result.isFailure)
        assertTrue(folder.resolve("blocked").exists())
    }

    @Test
    fun `No temporary file is left behind`() {
        folder.resolve("tones").createDirectories()
        saved("Rhythm", "Lead")
        store.rename("Lead", "Solo").getOrThrow()
        assertEquals(emptyList(), folder.resolve("tones").listDirectoryEntries(".*").map { it.name })
    }
}
