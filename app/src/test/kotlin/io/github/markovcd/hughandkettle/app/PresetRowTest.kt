package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.markovcd.hughandkettle.amp.AmpModels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class PresetRowTest {
    @get:Rule
    val compose = createComposeRule()

    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob())

    @After
    fun teardown() {
        scope.cancel()
        holder.close()
    }

    @Test
    fun `Stepping down from no known preset wraps to the last, and up from the last wraps to the first`() {
        val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
        compose.setContent { PresetRow(panel, enabled = true) }
        compose.onNodeWithTag("preset_down").performClick()
        compose.onNodeWithTag("preset").assertTextEquals("128")
        compose.onNodeWithTag("preset_up").performClick()
        compose.onNodeWithTag("preset").assertTextEquals("1")
        assertEquals(1, panel.state.preset)
    }
}
