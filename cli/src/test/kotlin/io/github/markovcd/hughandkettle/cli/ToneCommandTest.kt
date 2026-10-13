package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ToneCommandTest {
    private val folder: Path = Files.createTempDirectory("hk-tones")
    private val dir = folder.resolve("tones").toString()
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        fake.close()
        folder.deleteRecursively()
    }

    private class Run(
        val code: Int,
        val out: String,
        val err: String,
    )

    private fun run(vararg args: String): Run {
        val out = StringWriter()
        val err = StringWriter()
        val code = Cli.run(listOf("tone", *args, "--dir", dir), PrintWriter(out, true), PrintWriter(err, true))
        return Run(code, out.toString(), err.toString())
    }

    private fun ok(vararg args: String): String {
        val run = run(*args)
        assertEquals(ExitCodes.OK, run.code, run.err)
        return run.out.trim()
    }

    private fun names(): List<String> =
        ok("list")
            .lines()
            .filter {
                it.isNotEmpty()
            }.map { it.substringAfter("\"name\":\"").substringBefore('"') }

    @Test
    fun `A saved tone lists with its position, amp and values, typed as hk set takes them`() {
        val saved = ok("save", "black_spirit_200", "Rhythm", "gain=40", "channel=Crunch", "boost=on")
        assertEquals(
            """{"saved":"Rhythm","amp":"black_spirit_200","values":{"gain":40,"channel":47,"boost":127}}""",
            saved,
        )
        assertEquals(
            """{"position":1,"name":"Rhythm","amp":"black_spirit_200","values":{"channel":47,"boost":127,"gain":40}}""",
            ok("list"),
        )
    }

    @Test
    fun `A value a control does not take, or an unknown control, saves nothing`() {
        assertEquals(ExitCodes.BAD_ARGUMENTS, run("save", "black_spirit_200", "x", "gain=loud").code)
        val unknown = run("save", "black_spirit_200", "x", "flux=1")
        assertEquals(ExitCodes.BAD_ARGUMENTS, unknown.code)
        assertTrue("black_spirit_200 has channel" in unknown.err, unknown.err)
        assertEquals("", ok("list"))
    }

    @Test
    fun `A second tone of the same name needs --replace`() {
        ok("save", "black_spirit_200", "Rhythm", "gain=40")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run("save", "black_spirit_200", "rhythm", "gain=1").code)
        ok("save", "black_spirit_200", "Rhythm", "gain=1", "--replace")
        assertTrue("\"gain\":1}" in ok("list"))
    }

    @Test
    fun `Recalling a tone sends every value it holds to the WMI-1 in panel order`() {
        ok("save", "black_spirit_200", "Rhythm", "volume=90", "gain=40")
        val out = ok("recall", "127.0.0.1:${fake.port}", "rhythm")
        assertEquals(
            """{"host":"127.0.0.1","port":${fake.port},"tone":"Rhythm","amp":"black_spirit_200","sent":2}""",
            out,
        )
        val events = generateSequence { seen.poll(5, TimeUnit.SECONDS) }.take(3).toList()
        assertEquals(
            listOf(
                FakeWmiEvent.Received(ControlChange(MidiChannel(1), 20, 40)),
                FakeWmiEvent.Received(ControlChange(MidiChannel(1), 7, 90)),
            ),
            events.filterIsInstance<FakeWmiEvent.Received>(),
        )
    }

    @Test
    fun `A tone that is not there is the nothing-found code, with the names there are`() {
        ok("save", "black_spirit_200", "Rhythm", "gain=40")
        val runs =
            listOf(run("recall", "127.0.0.1:${fake.port}", "Lead"), run("delete", "Lead"), run("rename", "Lead", "x"))
        assertEquals(listOf(ExitCodes.NOTHING_FOUND), runs.map { it.code }.distinct())
        assertTrue(runs.all { "no tone 'Lead'; there are 'Rhythm'" in it.err }, runs.joinToString { it.err })
    }

    @Test
    fun `Tones rename, move and delete by name`() {
        listOf("A", "B", "C").forEach { ok("save", "black_spirit_200", it, "gain=1") }
        ok("rename", "a", "Verse")
        ok("move", "C", "1")
        ok("delete", "B")
        assertEquals(listOf("C", "Verse"), names())
    }

    @Test
    fun `An exported tone imports into another folder as the same tone`() {
        ok("save", "grandmeister_deluxe_40", "Lead", "gain=100", "power_soak=5 W")
        val file = folder.resolve("lead.json")
        ok("export", "Lead", file.toString())
        val other = folder.resolve("other").toString()
        val out = StringWriter()
        val code =
            Cli.run(
                listOf("tone", "import", file.toString(), "--dir", other),
                PrintWriter(out),
                PrintWriter(StringWriter()),
            )
        assertEquals(ExitCodes.OK, code)
        assertEquals("""{"imported":"Lead","skipped":[]}""", out.toString().trim())
        assertEquals(ok("list").substringAfter(','), run("list").out.trim().substringAfter(','))
        assertTrue(file.readText().contains("\"version\": 1"))
    }

    @Test
    fun `An import skips and reports a control the amp lacks, and refuses a newer file whole`() {
        val file = folder.resolve("t.json")
        file.writeText(
            """{"version": 1, "amp": "black_spirit_200", "name": "Odd", "values": {"gain": 3, "power_soak": 1}}""",
        )
        val run = run("import", file.toString())
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertEquals(
            """{"imported":"Odd","skipped":["Black Spirit 200 has no control 'power_soak'"]}""",
            run.out.trim(),
        )
        assertTrue("skipped: Black Spirit 200 has no control 'power_soak'" in run.err)
        file.writeText("""{"version": 9, "amp": "black_spirit_200", "name": "New", "values": {}}""")
        val newer = run("import", file.toString())
        assertEquals(ExitCodes.BAD_FILE, newer.code)
        assertTrue("from a newer Hugh & Kettle" in newer.err, newer.err)
        assertEquals(listOf("Odd"), names())
    }

    @Test
    fun `A tone command without its arguments prints the usage`() {
        val run = run("move", "A")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertTrue("hk tone import" in run.err)
    }
}
