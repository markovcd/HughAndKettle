package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SongCommandTest {
    private val folder: Path = Files.createTempDirectory("hk-songs")
    private val dir = folder.resolve("songs").toString()
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val time = AmpModels.grandMeisterDeluxe40.control("delay_time")!!

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
        val code = Cli.run(listOf("song", *args, "--dir", dir), PrintWriter(out, true), PrintWriter(err, true))
        return Run(code, out.toString(), err.toString())
    }

    private fun next(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    @Test
    fun `A song set reads back with its tempo, the delay time it makes and its steps`() {
        assertEquals(ExitCodes.OK, run("set", "grandmeister_deluxe_40", "120", "5:32", "7:16").code)
        assertEquals(
            listOf(
                """{"amp":"grandmeister_deluxe_40","name":"","tempo":120,"count_in":true,"delay_ms":505}""",
                """{"step":1,"preset":5,"beats":32}""",
                """{"step":2,"preset":7,"beats":16}""",
            ),
            run("show", "grandmeister_deluxe_40").out.lines().filter { it.isNotEmpty() },
        )
    }

    @Test
    fun `A song set with no count-in reads back without one`() {
        run("set", "grandmeister_deluxe_40", "120", "5:32", "--no-count-in")
        assertEquals(
            """{"amp":"grandmeister_deluxe_40","name":"","tempo":120,"count_in":false,"delay_ms":505}""",
            run("show", "grandmeister_deluxe_40").out.lines().first(),
        )
    }

    @Test
    fun `A step that is not preset and beats is refused before anything is saved`() {
        val run = run("set", "grandmeister_deluxe_40", "120", "5:32", "7")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertTrue("a step is <preset>:<beats>" in run.err, run.err)
        assertEquals(1, run("show", "grandmeister_deluxe_40").out.lines().count { it.isNotEmpty() })
    }

    @Test
    fun `A song counts in, plays each preset on its beat with the delay time of its tempo, and holds that time`() {
        // At 400 BPM a beat is 150 ms, which the time knob reaches as 154; a count-in of four and two
        // steps of two play in 1.2 s.
        run("set", "grandmeister_deluxe_40", "400", "3:2", "5:2")
        val playing = CompletableFuture.supplyAsync { run("play", "127.0.0.1:${fake.port}", "grandmeister_deluxe_40") }
        val delay = ControlChange(MidiChannel(1), 4, time.scale!!.value(150))
        assertTrue(next() is FakeWmiEvent.Opened)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 2)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        fake.emit(ValueBlock(mapOf(4 to 200), preset = 3).message())
        assertEquals(FakeWmiEvent.Received(delay), next())
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 4)), next())
        assertEquals(FakeWmiEvent.Received(delay), next())
        val run = playing.get(10, TimeUnit.SECONDS)
        assertEquals(ExitCodes.OK, run.code, run.err)
        val lines =
            run.out
                .lines()
                .filter { it.isNotEmpty() }
                .map { it.replace(Regex("""^\{"ms":-?\d+,"""), "{") }
        assertEquals(
            listOf(
                """{"count":4}""",
                """{"count":3}""",
                """{"count":2}""",
                """{"count":1}""",
                """{"step":1,"preset":3,"delay_ms":154}""",
                """{"restored":${delay.value},"delay_ms":154}""",
                """{"step":2,"preset":5,"delay_ms":154}""",
                """{"ended":true}""",
            ),
            lines,
        )
    }

    @Test
    fun `Songs set by name are listed in order, and the one without a name is the one open`() {
        run("set", "grandmeister_deluxe_40", "120", "5:32", "--name", "Highway Star")
        run("set", "grandmeister_deluxe_40", "96", "7:16", "7:16", "--name", "Slow blues")
        run("set", "grandmeister_deluxe_40", "100", "9:8")
        assertEquals(
            listOf(
                """{"name":"Highway Star","open":false,"tempo":120,"steps":1,"ms":16000}""",
                """{"name":"Slow blues","open":true,"tempo":100,"steps":1,"ms":4800}""",
            ),
            run("list", "grandmeister_deluxe_40").out.lines().filter { it.isNotEmpty() },
        )
        assertEquals(
            """{"amp":"grandmeister_deluxe_40","name":"Highway Star","tempo":120,"count_in":true,"delay_ms":505}""",
            run("show", "grandmeister_deluxe_40", "--name", "highway star").out.lines().first(),
        )
        val missing = run("show", "grandmeister_deluxe_40", "--name", "Encore")
        assertEquals(ExitCodes.BAD_ARGUMENTS, missing.code)
        assertTrue("no song named 'Encore'" in missing.err, missing.err)
    }

    @Test
    fun `An amp with no song has nothing to play`() {
        val run = run("play", "127.0.0.1:${fake.port}", "grandmeister_deluxe_40")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertTrue("has no song to play" in run.err, run.err)
    }
}
