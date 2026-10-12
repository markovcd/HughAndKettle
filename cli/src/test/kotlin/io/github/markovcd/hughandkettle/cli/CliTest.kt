package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.OtherMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import java.io.PrintWriter
import java.io.StringWriter
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds

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

    private fun next(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    private fun started(vararg args: String): CompletableFuture<Run> = CompletableFuture.supplyAsync { run(*args) }

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

    @Test
    fun `Listening prints every message the amp sends, and a link the WMI-1 drops ends it with the no-link code`() {
        val listening = started("listen", "127.0.0.1:${fake.port}", "--seconds", "5")
        assertTrue(next() is FakeWmiEvent.Opened)
        fake.emit(ControlChange(MidiChannel(1), 9, 127))
        fake.emit(ProgramChange(MidiChannel(1), 3))
        fake.close()
        val run = listening.get(10, TimeUnit.SECONDS)
        assertEquals(ExitCodes.NO_LINK, run.code, run.err)
        assertEquals(
            listOf(
                """"hex":"B0 09 7F","type":"control_change","channel":1,"controller":9,"value":127}""",
                """"hex":"C0 03","type":"program_change","channel":1,"program":3}""",
            ),
            run.out
                .lines()
                .filter { it.isNotEmpty() }
                .map { it.replace(Regex("""^\{"ms":\d+,"""), "") },
        )
        assertEquals("link lost: closed by the WMI-1", run.err.trim())
    }

    @Test
    fun `Listening with reconnect prints the link's states between the messages, and a dropped link is retried`() {
        // One second is the shortest --seconds takes; this test is that second long.
        val listening = started("listen", "127.0.0.1:${fake.port}", "--seconds", "1", "--reconnect")
        assertTrue(next() is FakeWmiEvent.Opened)
        fake.emit(ProgramChange(MidiChannel(1), 3))
        fake.close()
        val run = listening.get(10, TimeUnit.SECONDS)
        assertEquals(ExitCodes.OK, run.code, run.err)
        val address = "127.0.0.1:${fake.port}"
        val lines =
            run.out
                .lines()
                .filter { it.isNotEmpty() }
                .map { it.replace(Regex("""^\{"ms":\d+,"""), "") }
        // The message is printed by the link's reader, the states by the keeper, so only each kind is in order.
        assertEquals(
            listOf(
                """"link":"connecting","address":"$address","attempt":1}""",
                """"link":"connected","address":"$address"}""",
                """"link":"retrying","address":"$address","failures":1,"failure":"lost","reason":"closed by the WMI-1"}""",
            ),
            lines.filter { it.startsWith(""""link"""") }.take(3),
        )
        assertTrue(""""hex":"C0 03","type":"program_change","channel":1,"program":3}""" in lines, run.out)
    }

    @Test
    fun `Listening for N seconds to a quiet amp ends with the OK code and prints nothing`() {
        // One second is the shortest --seconds takes; this test is that second long.
        val run = run("listen", "127.0.0.1:${fake.port}", "--seconds", "1")
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertEquals("", run.out)
    }

    @Test
    fun `Listening for zero seconds is refused`() {
        val run = run("listen", "127.0.0.1:${fake.port}", "--seconds", "0")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
    }

    @Test
    fun `A send kept open shows what the WMI-1 sends back on stderr, banner text readable`() {
        val sending = started("send", "127.0.0.1:${fake.port}", "pc", "3", "--keep-open", "5", "--verbose")
        assertTrue(next() is FakeWmiEvent.Opened)
        assertEquals(FakeWmiEvent.Received(ProgramChange(MidiChannel(1), 3)), next())
        fake.emit(OtherMessage("*HELLO*".toByteArray(Charsets.US_ASCII)))
        fake.close()
        val run = sending.get(10, TimeUnit.SECONDS)
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertTrue(run.err.contains("received 2A 48 45 4C 4C 4F 2A |*HELLO*|"), run.err)
        assertTrue(run.err.contains("link lost: closed by the WMI-1"), run.err)
    }

    @Test
    fun `Discovery with --raw prints the packet ahead of the device it parsed to`() {
        val port = DatagramSocket(0).use { it.localPort }
        fake.broadcast(InetAddress.getLoopbackAddress(), port, interval = 100.milliseconds)
        // Capped at 5 s so a broken broadcaster fails rather than hangs; --first ends it at the first packet.
        val run = run("discover", "--port", "$port", "--seconds", "5", "--first", "--raw")
        assertEquals(ExitCodes.OK, run.code, run.err)
        val lines = run.out.lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size, run.out)
        assertTrue(
            lines[0].startsWith(
                """{"host":"127.0.0.1","length":110,"raw":"00 00 00 00 00 00 00 00 ${hex(fake.port)} """,
            ),
            lines[0],
        )
        assertTrue(lines[1].contains(""""device":"WMI-1_FAKE""""), lines[1])
    }

    @Test
    fun `Setting a control by name sends its controller and names it in the JSON line`() {
        val cases =
            listOf(
                listOf("gain", "100") to ControlChange(MidiChannel(1), 20, 100),
                listOf("boost", "on") to ControlChange(MidiChannel(1), 64, 127),
                listOf("channel", "lead") to ControlChange(MidiChannel(1), 31, 79),
            )
        cases.forEach { (setting, sent) ->
            val run = run("set", "127.0.0.1:${fake.port}", "black_spirit_200", *setting.toTypedArray())
            assertEquals(ExitCodes.OK, run.code, run.err)
            assertTrue(run.out.contains(""""amp":"black_spirit_200","control":"${setting[0]}","""), run.out)
            val events =
                generateSequence {
                    seen.poll(
                        5,
                        TimeUnit.SECONDS,
                    )
                }.takeWhile { it != FakeWmiEvent.Closed }.toList()
            assertTrue(FakeWmiEvent.Received(sent) in events, "$setting: $events")
        }
    }

    @Test
    fun `Recalling a preset on an amp with banks prints its bank and slot`() {
        val run = run("set", "127.0.0.1:${fake.port}", "grandmeister_deluxe_40", "preset", "6")
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertTrue(run.out.contains(""""preset":6,"bank":2,"slot":2,"hex":"C0 05""""), run.out)
    }

    @Test
    fun `A value a control does not take is refused with what it does take, before anything is sent`() {
        val run = run("set", "127.0.0.1:${fake.port}", "black_spirit_200", "channel", "metal")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertEquals("'channel' takes 'Clean', 'Crunch', 'Lead', 'Ultra', got 'metal'", run.err.trim())
        assertEquals(null, seen.poll(200, TimeUnit.MILLISECONDS), "a link was opened")
    }

    @Test
    fun `An unknown amp or control is refused with the ids there are`() {
        val noAmp = run("set", "127.0.0.1:${fake.port}", "plexi", "gain", "1")
        assertEquals(ExitCodes.BAD_ARGUMENTS, noAmp.code)
        assertTrue(noAmp.err.startsWith("no amp 'plexi'; there are black_spirit_200"), noAmp.err)
        val noControl = run("set", "127.0.0.1:${fake.port}", "black_spirit_200", "power_soak", "1")
        assertEquals(ExitCodes.BAD_ARGUMENTS, noControl.code)
        assertTrue(noControl.err.contains("has no control 'power_soak'"), noControl.err)
    }

    @Test
    fun `The state prints the preset and values the amp reported`() {
        // One second is the shortest --seconds takes; this test is that second long.
        val listening = started("state", "127.0.0.1:${fake.port}", "black_spirit_200", "--seconds", "1")
        assertTrue(next() is FakeWmiEvent.Opened)
        fake.emit(ProgramChange(MidiChannel(1), 4))
        fake.emit(ControlChange(MidiChannel(1), 20, 90))
        val run = listening.get(10, TimeUnit.SECONDS)
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertEquals("""{"amp":"black_spirit_200","preset":5,"values":{"gain":90}}""", run.out.trim())
    }

    @Test
    fun `Latency prints the spread of N messages to a fake WMI-1, smallest first`() {
        val run = run("latency", "--count", "20")
        assertEquals(ExitCodes.OK, run.code, run.err)
        val fields =
            Regex("\"(\\w+)_us\":(\\d+)").findAll(run.out).associate {
                it.groupValues[1] to
                    it.groupValues[2].toLong()
            }
        assertEquals(listOf("min", "median", "p95", "max"), fields.keys.toList(), run.out)
        assertEquals(fields.values.sorted(), fields.values.toList(), run.out)
        assertTrue(run.out.startsWith("""{"count":20,"""), run.out)
    }

    @Test
    fun `Latency over no messages is refused`() {
        assertEquals(ExitCodes.BAD_ARGUMENTS, run("latency", "--count", "0").code)
    }

    @Test
    fun `A fake WMI-1 run for N seconds stops by itself with the OK code`() {
        // One second is the shortest --seconds takes; this test is that second long.
        val run = run("fake-wmi", "--port", "0", "--no-broadcast", "--seconds", "1")
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertTrue(run.out.startsWith("""{"event":"listening","""), run.out)
    }

    @Test
    fun `A fake WMI-1 run for zero seconds is refused`() {
        assertEquals(ExitCodes.BAD_ARGUMENTS, run("fake-wmi", "--seconds", "0").code)
    }

    private fun hex(port: Int): String = "%02X %02X".format(port shr 8, port and 0xFF)
}
