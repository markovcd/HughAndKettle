package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.ToneStore
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
class ToneShelfCardTest {
    @get:Rule
    val compose = createComposeRule()

    private val folder: Path = Files.createTempDirectory("shelf-card")
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.blackSpirit200
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val shelf = ToneShelf(ToneStore(folder.resolve("tones")), panel, scope, Dispatchers.Unconfined)

    @After
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    @Test
    fun `Save is off until the panel knows a value`() {
        compose.setContent { ToneShelfCard(shelf, enabled = true) }
        compose.onNodeWithTag("tone_save").assertIsNotEnabled()
    }

    @Test
    fun `A tone saved under a typed name shows on the shelf, and its menu deletes it`() {
        panel.set(amp.control("gain")!!, 40)
        compose.setContent { ToneShelfCard(shelf, enabled = true) }
        compose.onNodeWithTag("tone_save").performClick()
        compose.onNodeWithTag("tone_name").performTextInput("Lead")
        compose.onNodeWithTag("tone_name_done").performClick()
        compose.onNodeWithTag("tone:Lead").assertExists()
        assertEquals(listOf("Lead"), shelf.tones.map { it.name })

        compose.onNodeWithTag("tone_menu:Lead").performClick()
        compose.onNodeWithTag("tone_delete").performClick()
        compose.onNodeWithTag("tone_delete_confirm").performClick()
        compose.onNodeWithTag("tone:Lead").assertDoesNotExist()
    }
}
