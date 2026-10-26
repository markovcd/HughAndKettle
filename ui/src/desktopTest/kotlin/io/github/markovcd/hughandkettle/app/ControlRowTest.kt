package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ControlRowTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob())

    @AfterTest
    fun teardown() {
        scope.cancel()
        holder.close()
    }

    @Test
    fun `Every control of every amp is described by its name`() =
        runComposeUiTest {
            val panels = AmpModels.all.map { PanelState(it, holder, scope, flowOf(null)) }
            setContent {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    panels.forEach { panel ->
                        panel.amp.controls.forEach { control ->
                            Box(Modifier.testTag("${panel.amp.id}/${control.id}")) {
                                ControlRow(listOf(control), panel, enabled = true)
                            }
                        }
                    }
                }
            }
            val undescribed =
                AmpModels.all.flatMap { amp ->
                    amp.controls
                        .filter { control ->
                            onAllNodes(
                                hasContentDescription(control.name) and
                                    hasAnyAncestor(hasTestTag("${amp.id}/${control.id}")),
                                useUnmergedTree = true,
                            ).fetchSemanticsNodes()
                                .isEmpty()
                        }.map { "${amp.id}/${it.id}" }
                }
            assertEquals(emptyList(), undescribed.take(5))
        }

    @Test
    fun `Delay time reads in milliseconds, with no tap or tempo of its own`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.blackSpirit200, holder, scope, flowOf(null))
            val row = listOf("delay_time", "delay_feedback").map { panel.amp.control(it)!! }
            panel.set(row[0], 44)
            setContent { ControlRow(row, panel, enabled = true) }
            onNodeWithTag("value:delay_time").assertTextEquals("505 ms")
            onNodeWithTag("value:delay_feedback").assertTextEquals("0")
            onNodeWithTag("tap:delay_time").assertDoesNotExist()
            onNodeWithTag("bpm:delay_time").assertDoesNotExist()
        }

    @Test
    fun `A knob, a switch and a selector the amp moves are each marked`() =
        runComposeUiTest {
            val panel = PanelState(AmpModels.blackSpirit200, holder, scope, flowOf(null))
            val rows =
                listOf(listOf("gain"), listOf("fx_loop"), listOf("channel")).map { ids ->
                    ids.map { panel.amp.control(it)!! }
                }
            FakeWmi(onEvent = {}).use { fake ->
                holder.connect(WmiAddress("127.0.0.1", fake.port))
                waitUntil(timeoutMillis = 5_000) { holder.state.value is LinkState.Connected }
                setContent { Column { rows.forEach { ControlRow(it, panel, enabled = true) } } }
                rows.flatten().forEach { fake.emit(ControlChange(MidiChannel(1), it.controller, 99)) }
                waitUntil(timeoutMillis = 5_000) { panel.moved.size == rows.size }
                val unmarked =
                    rows.flatten().map { it.id }.filter {
                        onAllNodes(hasTestTag("control:$it") and SemanticsMatcher.expectValue(MovedOnAmp, true))
                            .fetchSemanticsNodes()
                            .isEmpty()
                    }
                assertEquals(emptyList(), unmarked)
            }
        }
}
