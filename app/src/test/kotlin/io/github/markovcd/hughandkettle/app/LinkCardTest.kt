package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LinkCardTest {
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
    fun `Connecting with no address says what is missing and leaves the fields editable`() {
        val panel = PanelState(AmpModels.blackSpirit200, holder, scope)
        compose.setContent { LinkCard(panel, DiscoveryState(WmiDiscovery(0))) }
        compose.onNodeWithTag("connect").performClick()
        compose.onNodeWithTag("status").assertTextContains("Enter the WMI-1 address", substring = true)
        compose.onNodeWithTag("host").assertIsEnabled()
    }
}
