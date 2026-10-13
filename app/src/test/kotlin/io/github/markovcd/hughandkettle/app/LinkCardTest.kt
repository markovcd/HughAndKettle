package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
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
        val panel = PanelState(AmpModels.blackSpirit200, holder, scope, flowOf(null))
        compose.setContent { LinkCard(panel, DiscoveryState(WmiDiscovery(0))) }
        compose.onNodeWithTag("host").performTextClearance()
        compose.onNodeWithTag("connect").performClick()
        compose.onNodeWithTag("status").assertTextContains("Enter the WMI-1 address", substring = true)
        compose.onNodeWithTag("host").assertIsEnabled()
    }

    @Test
    fun `A plugged-in cable is offered while idle and a tap connects over it`() {
        val doremidi = Cable(7, "DOREMIDI")
        val plugged =
            LinkHolder(
                object : CableJack {
                    override val cables: StateFlow<List<Cable>> = MutableStateFlow(listOf(doremidi))

                    override fun open(
                        cable: Cable,
                        onMessage: (MidiMessage) -> Unit,
                        onLost: (reason: String) -> Unit,
                        onOpened: (Result<CableLink>) -> Unit,
                    ) = onOpened(Result.success(CableLink(cable, write = {}, release = {})))
                },
            )
        val panel = PanelState(AmpModels.blackSpirit200, plugged, scope, flowOf(null))
        compose.setContent { LinkCard(panel, DiscoveryState(WmiDiscovery(0))) }
        compose.onNodeWithTag("cable:7").performClick()
        compose.waitUntil(5_000) { panel.linked }
        compose.onNodeWithTag("status").assertTextContains("Connected over USB to DOREMIDI")
        compose.onNodeWithTag("disconnect").assertExists()
        plugged.close()
    }
}
