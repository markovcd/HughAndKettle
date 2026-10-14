package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNameStore
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

@OptIn(ExperimentalTestApi::class)
class PresetRowTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob())
    private val folder: Path = Files.createTempDirectory("preset-names")

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun names(panel: PanelState) =
        PresetNamesState(PresetNameStore(folder), panel, scope, Dispatchers.Unconfined)

    @Test
    fun `Stepping down from no known preset wraps to the last, and up from the last wraps to the first`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
            setContent { PresetRow(panel, names(panel), enabled = true) }
            onNodeWithTag("preset_down").performClick()
            onNodeWithTag("preset").assertTextEquals("128")
            onNodeWithTag("preset_up").performClick()
            onNodeWithTag("preset").assertTextEquals("1")
            assertEquals(1, panel.state.preset)
        }

    @Test
    fun `A tap on the preset opens a grid of every preset, and a tap there recalls it and closes the grid`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
            setContent { PresetRow(panel, names(panel), enabled = true) }
            onNodeWithTag("preset").performClick()
            onNodeWithTag("preset_grid").performScrollToNode(hasTestTag("preset:128"))
            onNodeWithTag("preset:128").performClick()
            onNodeWithTag("preset").assertTextEquals("128")
            onNodeWithTag("preset_grid").assertDoesNotExist()
        }

    @Test
    fun `A disabled panel does not open the preset grid`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
            setContent { PresetRow(panel, names(panel), enabled = false) }
            onNodeWithTag("preset").performClick()
            onNodeWithTag("preset_grid").assertDoesNotExist()
        }

    @Test
    fun `A preset named in the grid shows its name under the preset and in the grid`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
            val names = names(panel)
            setContent { PresetRow(panel, names, enabled = true) }
            onNodeWithTag("preset_up").performClick()
            onNodeWithTag("preset").performClick()
            onNodeWithTag("preset_naming").performClick()
            onNodeWithTag("preset:1").performClick()
            onNodeWithTag("tone_name").performTextReplacement("Lead solo")
            onNodeWithTag("tone_name_done").performClick()
            waitUntil(timeoutMillis = 5_000) { names.names[1] == "Lead solo" }
            onNodeWithTag("preset_grid").assertExists()
            onNodeWithTag("preset_naming").performClick()
            onNodeWithTag("preset:1").performClick()
            onNodeWithTag("preset_label").assertTextEquals("Lead solo")
            assertEquals("Lead solo", PresetNameStore(folder).names(AmpModels.grandMeisterDeluxe40).getOrThrow()[1])
        }
}
