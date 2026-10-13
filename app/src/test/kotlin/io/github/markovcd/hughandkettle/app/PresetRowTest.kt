package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PresetRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob())
    private val folder: Path = Files.createTempDirectory("preset-names")

    @After
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun names(panel: PanelState) =
        PresetNamesState(PresetNameStore(folder), panel, scope, Dispatchers.Unconfined)

    @Test
    fun `Stepping down from no known preset wraps to the last, and up from the last wraps to the first`() {
        val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
        compose.setContent { PresetRow(panel, names(panel), enabled = true) }
        compose.onNodeWithTag("preset_down").performClick()
        compose.onNodeWithTag("preset").assertTextEquals("128")
        compose.onNodeWithTag("preset_up").performClick()
        compose.onNodeWithTag("preset").assertTextEquals("1")
        assertEquals(1, panel.state.preset)
    }

    @Test
    fun `A tap on the preset opens a grid of every preset, and a tap there recalls it and closes the grid`() {
        val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
        compose.setContent { PresetRow(panel, names(panel), enabled = true) }
        compose.onNodeWithTag("preset").performClick()
        compose.onNodeWithTag("preset_grid").performScrollToNode(hasTestTag("preset:128"))
        compose.onNodeWithTag("preset:128").performClick()
        compose.onNodeWithTag("preset").assertTextEquals("128")
        compose.onNodeWithTag("preset_grid").assertDoesNotExist()
    }

    @Test
    fun `A disabled panel does not open the preset grid`() {
        val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
        compose.setContent { PresetRow(panel, names(panel), enabled = false) }
        compose.onNodeWithTag("preset").performClick()
        compose.onNodeWithTag("preset_grid").assertDoesNotExist()
    }

    @Test
    fun `A preset named in the grid shows its name under the preset and in the grid`() {
        val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
        val names = names(panel)
        compose.setContent { PresetRow(panel, names, enabled = true) }
        compose.onNodeWithTag("preset_up").performClick()
        compose.onNodeWithTag("preset").performClick()
        compose.onNodeWithTag("preset_naming").performClick()
        compose.onNodeWithTag("preset:1").performClick()
        compose.onNodeWithTag("tone_name").performTextReplacement("Lead solo")
        compose.onNodeWithTag("tone_name_done").performClick()
        compose.waitUntil(5_000) { names.names[1] == "Lead solo" }
        compose.onNodeWithTag("preset_grid").assertExists()
        compose.onNodeWithTag("preset_naming").performClick()
        compose.onNodeWithTag("preset:1").performClick()
        compose.onNodeWithTag("preset_label").assertTextEquals("Lead solo")
        assertEquals("Lead solo", PresetNameStore(folder).names(AmpModels.grandMeisterDeluxe40).getOrThrow()[1])
    }
}
