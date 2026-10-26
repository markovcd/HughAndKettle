package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
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
import kotlin.test.assertNotEquals

@OptIn(ExperimentalTestApi::class)
class FxTabTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.grandMeisterDeluxe40
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val folder: Path = Files.createTempDirectory("songs")
    private val songs = SongState(SongStore(folder), panel, scope, Dispatchers.Unconfined)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun ComposeUiTest.shown() = setContent { FxTab(panel, songs, enabled = true) }

    /** Modulation and delay on with every knob behind them at 64, reverb off. */
    private fun effectsOn() {
        listOf("modulation" to 127, "delay" to 127, "reverb" to 0).forEach { (id, value) ->
            panel.set(amp.control(id)!!, value)
        }
        amp.controls.filter { it.needs == "modulation" || it.needs == "delay" }.forEach { panel.set(it, 64) }
    }

    @Test
    fun `With every effect off the dice is disabled and says to turn one on`() =
        runComposeUiTest {
            shown()
            onNodeWithTag("fx_randomize").assertIsNotEnabled()
            onNodeWithText("Turn on an effect to randomize it.").assertExists()
        }

    @Test
    fun `The dice names the effects it changes and the locked time it keeps`() =
        runComposeUiTest {
            effectsOn()
            panel.lock(amp.control("delay_time")!!, on = true)
            shown()
            onNodeWithTag("fx_randomize").assertIsEnabled()
            onNodeWithText("Changes Modulation and Delay. Delay time stays").assertExists()
        }

    @Test
    fun `A roll offers an undo that puts the effects back and goes away`() =
        runComposeUiTest {
            effectsOn()
            val before = panel.state.values
            shown()
            onNodeWithTag("fx_randomize").performClick()
            assertNotEquals(before, panel.state.values)
            onNodeWithText("Modulation and Delay randomized").assertExists()
            onNodeWithTag("fx_randomize_undo").performClick()
            assertEquals(before, panel.state.values)
            onNodeWithTag("fx_randomize_undo").assertDoesNotExist()
        }
}
