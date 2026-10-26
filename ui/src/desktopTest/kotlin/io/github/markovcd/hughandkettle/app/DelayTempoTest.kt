package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.SongStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalTestApi::class)
class DelayTempoTest {
    private val folder: Path = Files.createTempDirectory("songs")
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.grandMeisterDeluxe40
    private val time = amp.control("delay_time")!!
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val songs = SongState(SongStore(folder), panel, scope, Dispatchers.Unconfined)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun ComposeUiTest.shown() =
        setContent {
            EffectCard(
                amp.control("delay")!!,
                amp.controls.filter { it.needs == "delay" },
                panel,
                songs,
                enabled = true,
                open = true,
                onOpen = {},
            )
        }

    private fun at(ms: Int) = time.scale!!.value(ms)

    @Test
    fun `Off, the delay is a plain time in milliseconds, and locked it is a note at the song's tempo`() =
        runComposeUiTest {
            panel.set(amp.control("delay")!!, 127)
            panel.set(time, at(876))
            shown()
            onNodeWithTag("control:delay_time").assertExists()
            onNodeWithTag("note_length:1/4").assertDoesNotExist()
            onNodeWithTag("lock:delay_time").performClick()
            assertEquals(at(500), panel.lock?.value)
            onNodeWithTag("control:delay_time").assertDoesNotExist()
            onNodeWithTag("value:delay_time").assertTextEquals("1/4")
            onNodeWithTag("fx_tempo").assertTextEquals("at 120 BPM")
            onNodeWithTag("note_length:1/8d").performClick()
            assertEquals(at(375), panel.state.values["delay_time"])
            onNodeWithTag("value:delay_time").assertTextEquals("Dotted 1/8")
            onNodeWithTag("lock:delay_time").performClick()
            assertNull(panel.lock)
            onNodeWithTag("control:delay_time").assertExists()
        }

    @Test
    fun `A note too long for the tempo is disabled, and a picked one says the delay holds at its longest`() =
        runComposeUiTest {
            panel.set(amp.control("delay")!!, 127)
            songs.lock(on = true)
            songs.note(NoteLength.of("1/4d")!!)
            songs.tempo(60)
            shown()
            onNodeWithTag("note_length:1/2").assertIsNotEnabled()
            onNodeWithTag("note_length:1/4d").assertIsEnabled()
            onNodeWithTag("value:delay_time").assertTextEquals("Too long, held at longest")
            onNodeWithTag("note_alert").assertExists()
            assertEquals(127, panel.state.values["delay_time"])
        }

    @Test
    fun `A closed card with its time locked names the note and the tempo`() =
        runComposeUiTest {
            songs.lock(on = true)
            setContent {
                EffectCard(
                    amp.control("delay")!!,
                    amp.controls.filter { it.needs == "delay" },
                    panel,
                    songs,
                    enabled = true,
                    open = false,
                    onOpen = {},
                )
            }
            onNodeWithTag("locked:delay", useUnmergedTree = true).assertExists()
        }
}
