package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class ControlRowTest {
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
    fun `Every control of every amp is described by its name`() {
        val panels = AmpModels.all.map { PanelState(it, holder, scope, flowOf(null)) }
        compose.setContent {
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
                        compose
                            .onAllNodes(
                                hasContentDescription(control.name) and
                                    hasAnyAncestor(hasTestTag("${amp.id}/${control.id}")),
                                useUnmergedTree = true,
                            ).fetchSemanticsNodes()
                            .isEmpty()
                    }.map { "${amp.id}/${it.id}" }
            }
        assertEquals(emptyList(), undescribed.take(5))
    }
}
