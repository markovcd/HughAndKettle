package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.app.theme.HughAndKettleTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class KnobTest {
    /** The values the knob asked for, in order, after [act] on a knob drawn at [start]. */
    private fun heard(
        start: Int,
        act: SemanticsNodeInteraction.() -> Unit,
    ): List<Int> {
        val asked = mutableListOf<Int>()
        runComposeUiTest {
            setContent {
                HughAndKettleTheme(dark = true) {
                    // In a pane with more to scroll, as on the panel, which must not take the wheel from the knob.
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        Knob(
                            value = start,
                            onValue = { asked += it },
                            description = "Gain",
                            modifier = Modifier.testTag("knob"),
                        )
                    }
                }
            }
            onNode(hasTestTag("knob")).act()
        }
        return asked
    }

    @Test
    fun `A knob set through its semantics asks for that value, clamped to 127`() {
        assertEquals(listOf(100), heard(10) { performSemanticsAction(SemanticsActions.SetProgress) { it(100f) } })
        assertEquals(listOf(127), heard(10) { performSemanticsAction(SemanticsActions.SetProgress) { it(500f) } })
    }

    @Test
    fun `A focused knob turns one step per arrow key and eight per page key on the desktop`() {
        assertEquals(listOf(11), heard(10) { requestFocus().performKeyInput { pressKey(Key.DirectionUp) } })
        assertEquals(listOf(2), heard(10) { requestFocus().performKeyInput { pressKey(Key.PageDown) } })
    }

    @Test
    fun `The mouse wheel turns a knob a step a notch, up when turned away`() {
        assertEquals(
            listOf(11),
            heard(10) {
                performMouseInput {
                    moveTo(center)
                    scroll(-1f)
                }
            },
        )
        assertEquals(
            listOf(9),
            heard(10) {
                performMouseInput {
                    moveTo(center)
                    scroll(1f)
                }
            },
        )
    }
}
