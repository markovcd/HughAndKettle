package io.github.markovcd.hughandkettle.desktop

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.ThemeChoice
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@OptIn(ExperimentalTestApi::class)
class DesktopPanelTest {
    private val folder: Path = Files.createTempDirectory("desktop")
    private val seen = LinkedBlockingQueue<FakeWmiEvent>()
    private val fake = FakeWmi(onEvent = seen::put)
    private val box = WmiAddress("127.0.0.1", fake.port)
    private val app = DesktopApp(folder, JavaCableJack(find = { Result.success(emptyList()) }))
    private lateinit var screen: DesktopScreen

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        app.close()
        fake.close()
        folder.deleteRecursively()
    }

    /** Runs [block] on a window the size the app opens at. */
    private fun inWindow(block: ComposeUiTest.() -> Unit) = runDesktopComposeUiTest(1440, 900) { block() }

    private fun nextSeen(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    private fun nextHeard(): MidiMessage {
        val event = nextSeen()
        return (event as? FakeWmiEvent.Received)?.message ?: nextHeard()
    }

    /** Waits, moving the window's clock, until the box hears a message [matching]. */
    private fun ComposeUiTest.heard(
        what: String,
        matching: (MidiMessage) -> Boolean,
    ) {
        val heard = mutableListOf<FakeWmiEvent>()
        waitUntil(what, timeoutMillis = 10_000) {
            seen.drainTo(heard)
            heard.any { it is FakeWmiEvent.Received && matching(it.message) }
        }
    }

    /** Shows the window and links it to the fake box by its typed address. */
    private fun ComposeUiTest.linked() {
        runBlocking { app.home.amps.remember(box, AmpModels.blackSpirit200) }
        setContent {
            screen = rememberDesktopScreen(app)
            DesktopPanel(screen, ThemeChoice.Dark)
        }
        onNodeWithTag("enter_address").performClick()
        onNodeWithTag("host").performTextReplacement(box.host)
        onNodeWithTag("port").performTextReplacement(box.port.toString())
        onNodeWithTag("connect").performClick()
        assertEquals(FakeWmiEvent.Opened::class, nextSeen()::class)
        waitUntil("the controls are enabled", timeoutMillis = 5_000) {
            onAllNodes(hasTestTag("control:mute") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun `A box typed in is linked to, remembered, and the panel's switches reach it`() =
        inWindow {
            linked()
            onNodeWithTag("control:mute").performClick()
            assertEquals(ControlChange(MidiChannel(1), 9, 127), nextHeard())
            assertEquals(
                box,
                runBlocking {
                    app.home.lastWmi.last
                        .first { it != null }
                },
            )
        }

    @Test
    fun `The amp, its effects, the song and the tones are all in view, with no tabs`() =
        inWindow {
            linked()
            onNode(hasTestTag("control:gain") and hasAnyAncestor(hasTestTag("amp"))).assertExists()
            onNode(hasTestTag("control:delay") and hasAnyAncestor(hasTestTag("fx"))).assertExists()
            onNode(hasTestTag("song_add") and hasAnyAncestor(hasTestTag("song"))).assertExists()
            onNode(hasTestTag("tone_save") and hasAnyAncestor(hasTestTag("tones"))).assertExists()
            onNodeWithTag("tab:amp").assertDoesNotExist()
            onNodeWithTag("settings").assertDoesNotExist()
        }

    @Test
    fun `The mouse wheel on a knob turns it on the box`() =
        inWindow {
            linked()
            val gain = AmpModels.blackSpirit200.control("gain")!!
            onNode(
                hasContentDescription(gain.name) and hasAnyAncestor(hasTestTag("amp")),
            ).performMouseInput {
                // The wheel turns where the pointer is, so it goes to the knob first.
                moveTo(center)
                scroll(-1f)
            }
            assertEquals(ControlChange(MidiChannel(1), gain.controller, 1), nextHeard())
        }

    @Test
    fun `A right-click on a tone opens its menu`() =
        inWindow {
            linked()
            screen.panel.set(AmpModels.blackSpirit200.control("gain")!!, 20)
            screen.shelf.save("Clean")
            waitUntil("the tone is saved", timeoutMillis = 5_000) { screen.shelf.tones.isNotEmpty() }
            onNodeWithTag("tone:Clean").performScrollTo().performMouseInput { rightClick() }
            onNodeWithTag("tone_delete").assertExists()
        }

    @Test
    fun `The tone shortcuts step through the tones on the box`() =
        inWindow {
            linked()
            val gain = AmpModels.blackSpirit200.control("gain")!!
            listOf(20, 40).forEachIndexed { i, value ->
                screen.panel.set(gain, value)
                screen.shelf.save("Gain $value")
                waitUntil("the tone is saved", timeoutMillis = 5_000) { screen.shelf.tones.size == i + 1 }
            }
            seen.clear()
            screen.commands.nextTone()
            heard("the first tone") { it == ControlChange(MidiChannel(1), gain.controller, 20) }
            screen.commands.nextTone()
            heard("the second tone") { it == ControlChange(MidiChannel(1), gain.controller, 40) }
        }

    @Test
    fun `Two taps set the delay time on the box to their beat`() =
        inWindow {
            linked()
            screen.commands.tap(kotlin.time.Duration.ZERO)
            screen.commands.tap(kotlin.time.Duration.parse("500ms"))
            val time = AmpModels.blackSpirit200.controls.first { it.timed }
            heard("the delay time") { it is ControlChange && it.controller == time.controller }
        }

    @Test
    fun `A song played in the window recalls its preset on the box`() =
        inWindow {
            linked()
            onNodeWithTag("song_add").performClick()
            waitUntil("the song has its step", timeoutMillis = 5_000) {
                screen.songs.song.steps
                    .isNotEmpty()
            }
            screen.commands.playOrStop()
            // The song's clock runs on the window's, which moves only while the test waits on it.
            heard("a preset is recalled") { it is ProgramChange }
        }
}
