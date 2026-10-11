package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CliTest {
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)

    @AfterTest
    fun teardown() = fake.close()

    private class Run(
        val code: Int,
        val out: String,
        val err: String,
    )

    private fun run(vararg args: String): Run {
        val out = StringWriter()
        val err = StringWriter()
        val code = Cli.run(args.toList(), PrintWriter(out, true), PrintWriter(err, true))
        return Run(code, out.toString(), err.toString())
    }

    @Test
    fun `No command prints the usage on stderr and exits with the bad-arguments code`() {
        val run = run()
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertEquals("", run.out)
        assertTrue(run.err.contains("hk send"))
    }

    @Test
    fun `Sending a control change reaches the WMI-1 and prints one JSON line`() {
        val run = run("send", "127.0.0.1:${fake.port}", "cc", "20", "100", "--channel", "2")
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertEquals(
            """{"host":"127.0.0.1","port":${fake.port},"hex":"B1 14 64","type":"control_change","channel":2,"controller":20,"value":100}""",
            run.out.trim(),
        )
        val events = generateSequence { seen.poll(5, TimeUnit.SECONDS) }.take(3).toList()
        assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(2), 20, 100)), events[1])
    }

    @Test
    fun `A value outside 0 to 127 is refused before anything is sent`() {
        val run = run("send", "127.0.0.1:${fake.port}", "cc", "20", "128")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertEquals("", run.out)
    }

    @Test
    fun `A host that does not answer exits with the no-link code`() {
        val run = run("send", "127.0.0.1:1", "pc", "3")
        assertEquals(ExitCodes.NO_LINK, run.code)
        assertTrue(run.err.startsWith("cannot reach 127.0.0.1:1"))
    }
}
