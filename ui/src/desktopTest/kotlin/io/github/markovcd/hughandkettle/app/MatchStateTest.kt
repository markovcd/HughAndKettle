package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LevelFrame
import io.github.markovcd.hughandkettle.amp.LoudnessCorrectionStore
import io.github.markovcd.hughandkettle.amp.LoudnessCorrections
import io.github.markovcd.hughandkettle.amp.LoudnessMatch
import io.github.markovcd.hughandkettle.amp.MatchResult
import io.github.markovcd.hughandkettle.amp.Microphone
import io.github.markovcd.hughandkettle.amp.MicrophoneInput
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

class MatchStateTest {
    private val amp = AmpModels.grandMeisterDeluxe40
    private val volume = amp.control("volume")!!
    private val folder: Path = Files.createTempDirectory("match")
    private val store = LoudnessCorrectionStore(folder.resolve("loudness"))
    private val fake = FakeWmi(onEvent = ::answer)
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val panel = PanelState(amp, holder, scope, MutableStateFlow<WmiAddress?>(null))
    private val names = PresetNames(amp, mapOf(5 to "Clean verse", 7 to "Lead solo", 9 to "Big lead"))
    private var song = Song(amp, steps = listOf(SongStep(7, 16), SongStep(5, 16)))
    private val microphone = PushedMicrophone()

    /** Each preset's stored volume, as the fake amp recalls it. */
    private val stored = mapOf(5 to 64, 7 to 60, 9 to 118)

    /** The volume the fake amp plays now, as the app last set it or a recall put it. */
    @Volatile private var playing = 0

    /** Whether the fake amp answers a recall. */
    @Volatile private var answering = true

    private fun match(settle: kotlin.time.Duration = 1.milliseconds) =
        MatchState(
            store,
            panel,
            scope,
            { names },
            { song },
            Dispatchers.Unconfined,
            settle = settle,
            answerLimit = 2.seconds,
        )

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
        folder.deleteRecursively()
    }

    /** The fake amp: a recall answered with the preset's value block, a volume sent taken up. */
    private fun answer(event: FakeWmiEvent) {
        val message = (event as? FakeWmiEvent.Received)?.message ?: return
        when {
            message is ProgramChange && answering -> {
                val preset = message.program + 1
                playing = stored[preset] ?: 64
                fake.emit(ValueBlock(mapOf(volume.controller to 2 * playing), preset = preset).message())
            }

            message is ControlChange && message.controller == volume.controller -> {
                playing = message.value
            }
        }
    }

    /** Waits, up to 5 s, for [holds] to come true. */
    private fun until(
        what: String,
        holds: () -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!holds()) {
            if (started.elapsedNow() > 5.seconds) fail("never got to: $what")
            Thread.sleep(2)
        }
    }

    private fun connected() {
        panel.host = "127.0.0.1"
        panel.port = "${fake.port}"
        panel.connect()
        until("connected") { panel.link is LinkState.Connected }
    }

    /**
     * Plays a chord for each listen until the soundcheck is done: a quiet room, then a chord at [loudness] of the
     * preset and the volume it plays at.
     */
    private fun played(
        state: MatchState,
        loudness: (preset: Int, volume: Int) -> Double,
    ) {
        repeat(MOST_LISTENS) {
            until("a listen or the summary") {
                val stage = state.match?.stage
                stage is LoudnessMatch.Stage.Playing || stage is LoudnessMatch.Stage.Retry ||
                    stage == LoudnessMatch.Stage.Done
            }
            val now = state.match!!
            if (now.stage == LoudnessMatch.Stage.Done) return
            val level = loudness(now.at!!, playing)
            microphone.push(List(12) { -70.0 } + List(5) { level + 6 } + List(32) { level })
            until("the chord heard") { state.match !== now }
        }
        fail("the soundcheck never ended")
    }

    @Test
    fun `The set-up offers the open song's presets, the one playing the reference`() {
        connected()
        val state = match()
        panel.recall(5)
        until("preset 5 on") { panel.state.preset == 5 }
        state.open()
        assertEquals(listOf(7, 5), state.offered)
        assertEquals(setOf(7, 5), state.picked)
        assertEquals(5, state.reference)
        state.pickEveryNamed(true)
        assertEquals(listOf(5, 7, 9), state.offered)
    }

    @Test
    fun `A soundcheck brings each preset to the reference and keeps the corrections`() {
        connected()
        val state = match()
        state.open()
        state.refer(5)
        state.start(microphone)
        // Preset 7 is 3.85 dB louder than preset 5 at its stored volume, a third of a dB a step.
        played(state) { preset, volume -> if (preset == 5) -20.0 else -20.0 + 0.35 * (volume - 49) }
        val results = state.match!!.results
        assertIs<MatchResult.Reference>(results[0])
        assertEquals(MatchResult.Matched(7, 60, 49, -11, 0.0, true), results[1])
        state.keep()
        until("kept") { state.corrections.amounts.isNotEmpty() }
        assertEquals(mapOf(7 to -11), store.corrections(amp).getOrThrow().amounts)
        assertEquals(mapOf(7 to -11), panel.trim?.corrections)
        assertEquals(false, state.shown)
    }

    @Test
    fun `A preset cleared on the summary loses the correction it had`() {
        store.keep(LoudnessCorrections(amp, mapOf(7 to -4))).getOrThrow()
        connected()
        val state = match()
        until("read") { state.corrections.amounts.isNotEmpty() }
        state.open()
        state.start(microphone)
        played(state) { preset, volume -> if (preset == 5) -20.0 else -20.0 + 0.35 * (volume - 49) }
        state.clear(7)
        state.keep()
        until("kept") { state.corrections.amounts.isEmpty() }
        assertEquals(emptyMap(), store.corrections(amp).getOrThrow().amounts)
    }

    @Test
    fun `A soundcheck the amp never answers ends on its summary and says why`() {
        val state = match()
        state.open()
        state.refer(5)
        assertEquals(false, state.startable, "no link to start over")
        connected()
        answering = false
        state.start(microphone)
        until("the summary") { state.match?.stage == LoudnessMatch.Stage.Done }
        assertTrue(state.failure!!.contains("preset 5"), state.failure)
    }

    @Test
    fun `Closing the sheet mid-soundcheck keeps nothing and lets the microphone go`() {
        connected()
        val state = match()
        state.open()
        state.start(microphone)
        until("listening") { state.match?.stage is LoudnessMatch.Stage.Playing }
        state.close()
        until("the microphone let go") { microphone.closed }
        assertEquals(null, state.match)
        assertEquals(emptyMap(), store.corrections(amp).getOrThrow().amounts)
    }

    /** A microphone that hears what the test pushes, one frame per level. */
    private class PushedMicrophone : Microphone {
        private val frames = LinkedBlockingQueue<LevelFrame>()

        @Volatile var closed = false

        fun push(levels: List<Double>) = levels.forEach { frames.put(LevelFrame(it, 0.5)) }

        override fun open(): Result<MicrophoneInput> =
            Result.success(
                object : MicrophoneInput {
                    override fun read(): Result<LevelFrame> {
                        while (!closed) {
                            frames.poll(10, TimeUnit.MILLISECONDS)?.let { return Result.success(it) }
                        }
                        return Result.failure(IllegalStateException("closed"))
                    }

                    override fun close() {
                        closed = true
                    }
                },
            )
    }

    private companion object {
        // A match takes at most five listens a preset; three presets with room to spare.
        const val MOST_LISTENS = 20
    }
}
