package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LevelFrame
import io.github.markovcd.hughandkettle.amp.LoudnessCorrectionStore
import io.github.markovcd.hughandkettle.amp.LoudnessCorrections
import io.github.markovcd.hughandkettle.amp.Microphone
import io.github.markovcd.hughandkettle.amp.MicrophoneInput
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.ProgramChange
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
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MatchCommandTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val folder: Path = Files.createTempDirectory("match")
    private val store = LoudnessCorrectionStore(folder)
    private val frames = LinkedBlockingQueue<LevelFrame>()
    private val fake = FakeWmi(onEvent = ::answer)

    /** Each preset's stored volume, as the fake amp recalls it. */
    private val stored = mapOf(5 to 64, 7 to 60)

    /** The preset the fake amp is on. */
    @Volatile private var on = 0

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        fake.close()
        folder.deleteRecursively()
    }

    /**
     * The fake amp, and the guitarist in front of it: a recall is answered with the preset's value block, and each
     * time the volume settles a chord is played at a loudness of preset 7 3.85 dB over preset 5, a third of a dB
     * a volume step.
     */
    private fun answer(event: FakeWmiEvent) {
        val message = (event as? FakeWmiEvent.Received)?.message ?: return
        val volume =
            when {
                message is ProgramChange -> {
                    on = message.program + 1
                    fake.emit(ValueBlock(mapOf(57 to 2 * stored.getValue(on)), preset = on).message())
                    stored.getValue(on)
                }

                message is ControlChange && message.controller == 57 -> {
                    message.value
                }

                else -> {
                    return
                }
            }
        val level = if (on == 5) -20.0 else -20.0 + 0.35 * (volume - 49)
        // The room for longer than the command lets pass after a volume moves, then the chord.
        (List(40) { -70.0 } + List(5) { level + 6 } + List(32) { level }).forEach { frames.put(LevelFrame(it, 0.5)) }
    }

    private val microphone =
        Microphone {
            Result.success(
                object : MicrophoneInput {
                    override fun read(): Result<LevelFrame> =
                        frames.poll(5, TimeUnit.SECONDS)?.let { Result.success(it) }
                            ?: Result.failure(IllegalStateException("nothing heard in 5 s"))

                    override fun close() = Unit
                },
            )
        }

    private class Run(
        val code: Int,
        val out: String,
        val err: String,
    )

    private fun run(vararg args: String): Run {
        val out = StringWriter()
        val err = StringWriter()
        val options = Options.parse(args.toList(), setOf("presets", "reference", "dir"))
        val code = MatchCommand(microphone).run(options, PrintWriter(out, true), PrintWriter(err, true))
        return Run(code, out.toString(), err.toString())
    }

    @Test
    fun `A soundcheck matches each preset to the reference and keeps the corrections`() {
        val run = run("run", "127.0.0.1:${fake.port}", amp.id, "--presets", "5,7", "--dir", "$folder")
        assertEquals(ExitCodes.OK, run.code, run.err)
        assertEquals(
            listOf(
                """{"preset":5,"result":"reference","stored":64,"volume":64}""",
                """{"preset":7,"result":"matched","stored":60,"volume":49,"correction":-11,"off_db":0.0}""",
            ),
            run.out.lines().filter { it.isNotBlank() },
        )
        assertTrue("volume 60 -> 49" in run.err, run.err)
        assertEquals(mapOf(7 to -11), store.corrections(amp).getOrThrow().amounts)
    }

    @Test
    fun `Corrections kept show one line each and clear one at a time`() {
        store.keep(LoudnessCorrections(amp, mapOf(7 to -11, 9 to 4))).getOrThrow()
        assertEquals(
            """{"preset":7,"correction":-11}
            |{"preset":9,"correction":4}
            """.trimMargin(),
            run("show", amp.id, "--dir", "$folder").out.trim(),
        )
        assertEquals(ExitCodes.OK, run("clear", amp.id, "7", "--dir", "$folder").code)
        assertEquals(mapOf(9 to 4), store.corrections(amp).getOrThrow().amounts)
    }

    @Test
    fun `A soundcheck with no presets named is refused before anything is sent`() {
        val run = run("run", "127.0.0.1:${fake.port}", amp.id, "--dir", "$folder")
        assertEquals(ExitCodes.BAD_ARGUMENTS, run.code)
        assertTrue("--presets" in run.err, run.err)
    }
}
