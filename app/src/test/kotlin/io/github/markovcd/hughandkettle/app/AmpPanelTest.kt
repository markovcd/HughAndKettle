package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
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
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively

@RunWith(RobolectricTestRunner::class)
class AmpPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private val folder: Path = Files.createTempDirectory("panel")
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val panel = PanelState(AmpModels.blackSpirit200, holder, scope, flowOf(null))
    private val shelf = ToneShelf(ToneStore(folder.resolve("tones")), panel, scope, Dispatchers.Unconfined)

    @After
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun shown() =
        compose.setContent {
            AmpPanel(panel, DiscoveryState(WmiDiscovery(0)), shelf, ThemeChoice.Dark, onTheme = {})
        }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `A phone shows one column with the controls under the link`() {
        shown()
        compose.onNodeWithTag("panel").assertExists()
        compose.onNodeWithTag("controls").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w1280dp-h800dp-land")
    fun `A tablet in landscape gives the controls a pane of their own beside the link`() {
        shown()
        compose.onNodeWithTag("panel").assertExists()
        compose.onNodeWithTag("controls").assertExists()
        compose.onNodeWithTag("control:gain").assertExists()
    }
}
