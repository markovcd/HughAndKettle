package io.github.markovcd.hughandkettle.desktop

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
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

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        app.close()
        fake.close()
        folder.deleteRecursively()
    }

    private fun nextSeen(): FakeWmiEvent = assertNotNull(seen.poll(5, TimeUnit.SECONDS), "nothing arrived within 5 s")

    /** Shows the window and links it to the fake box by its typed address. */
    private fun ComposeUiTest.linked() {
        runBlocking { app.home.amps.remember(box, AmpModels.blackSpirit200) }
        setContent { DesktopPanel(app) }
        onNodeWithTag("enter_address").performClick()
        onNodeWithTag("host").performTextReplacement(box.host)
        onNodeWithTag("port").performTextReplacement(box.port.toString())
        onNodeWithTag("connect").performClick()
        assertEquals(FakeWmiEvent.Opened::class, nextSeen()::class)
    }

    @Test
    fun `A box typed in is linked to, remembered, and the panel's switches reach it`() =
        runComposeUiTest {
            linked()
            waitUntil("the switches are enabled", timeoutMillis = 5_000) {
                onAllNodes(hasTestTag("control:mute") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("control:mute").performClick()
            assertEquals(FakeWmiEvent.Received(ControlChange(MidiChannel(1), 9, 127)), nextSeen())
            assertEquals(
                box,
                runBlocking {
                    app.home.lastWmi.last
                        .first { it != null }
                },
            )
        }

    @Test
    fun `A song played in the window recalls its preset on the box`() =
        runComposeUiTest {
            linked()
            onNodeWithTag("tab:song").performClick()
            onNodeWithTag("song_add").performClick()
            waitUntil("the song can be played", timeoutMillis = 5_000) {
                onAllNodes(hasTestTag("song_play") and isEnabled()).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("song_play").performClick()
            // The song's clock runs on the window's, which moves only while the test waits on it.
            val heard = mutableListOf<FakeWmiEvent>()
            waitUntil("a preset is recalled", timeoutMillis = 10_000) {
                seen.drainTo(heard)
                heard.any { it is FakeWmiEvent.Received && it.message is ProgramChange }
            }
        }
}
