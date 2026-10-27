package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LfoShape
import io.github.markovcd.hughandkettle.amp.SongStore
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
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class FxTabTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val amp = AmpModels.grandMeisterDeluxe40
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val folder: Path = Files.createTempDirectory("songs")
    private val songs = SongState(SongStore(folder), panel, scope, Dispatchers.Unconfined)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun ComposeUiTest.shown() = setContent { FxTab(panel, songs, enabled = true) }

    /** Modulation and delay on with every knob behind them at 64, reverb off. */
    private fun effectsOn() {
        listOf("modulation" to 127, "delay" to 127, "reverb" to 0).forEach { (id, value) ->
            panel.set(amp.control(id)!!, value)
        }
        amp.controls.filter { it.needs == "modulation" || it.needs == "delay" }.forEach { panel.set(it, 64) }
    }

    @Test
    fun `With every effect off the dice is disabled and says to turn one on`() =
        runComposeUiTest {
            shown()
            onNodeWithTag("fx_randomize").assertIsNotEnabled()
            onNodeWithText("Turn on an effect to randomize it.").assertExists()
        }

    @Test
    fun `The dice names the effects it changes and the locked time it keeps`() =
        runComposeUiTest {
            effectsOn()
            panel.lock(amp.control("delay_time")!!, on = true)
            shown()
            onNodeWithTag("fx_randomize").assertIsEnabled()
            onNodeWithText("Changes Modulation and Delay. Delay time stays").assertExists()
        }

    @Test
    fun `In a narrow pane the locked word stays on one line beside its padlock`() =
        runComposeUiTest {
            effectsOn()
            panel.set(amp.control("reverb")!!, 127)
            panel.lock(amp.control("delay_time")!!, on = true)
            setContent { Box(Modifier.width(300.dp)) { RandomizeCard(panel, enabled = true) } }
            val locked = onNodeWithTag("fx_randomize_locked").getUnclippedBoundsInRoot()
            val wide = locked.right - locked.left
            val tall = locked.bottom - locked.top
            assertTrue(wide > tall, "locked. is $wide wide and $tall tall")
        }

    @Test
    fun `A roll offers an undo that puts the effects back and goes away`() =
        runComposeUiTest {
            effectsOn()
            val before = panel.state.values
            shown()
            onNodeWithTag("fx_randomize").performClick()
            assertNotEquals(before, panel.state.values)
            onNodeWithText("Modulation and Delay randomized").assertExists()
            onNodeWithTag("fx_randomize_undo").performClick()
            assertEquals(before, panel.state.values)
            onNodeWithTag("fx_randomize_undo").assertDoesNotExist()
        }

    /** Reverb on with its volume at 50, the reverb card open on the tab. */
    private fun ComposeUiTest.reverbOpen() {
        panel.set(amp.control("reverb")!!, 127)
        panel.set(amp.control("reverb_volume")!!, 50)
        shown()
        onNodeWithTag("effect:reverb").performClick()
    }

    @Test
    fun `An LFO switched on shows its swing in place of the knob it moves, and takes a shape`() =
        runComposeUiTest {
            reverbOpen()
            onNodeWithTag("fx_lfo_reverb_range_low").assertDoesNotExist()
            onNodeWithTag("fx_lfo_reverb").performClick()
            onNodeWithTag("fx_lfo_reverb_range_low").assertExists()
            onNodeWithText("18 – 82").assertExists()
            onNodeWithTag("fx_lfo_reverb_target:volume").assertDoesNotExist()
            onNodeWithTag("fx_lfo_reverb_shape:square").performClick()
            assertEquals(
                LfoShape.Square,
                panel.lfos.running
                    .getValue("reverb")
                    .shape,
            )
            onNodeWithTag("fx_lfo_reverb_range_high").performSemanticsAction(SemanticsActions.SetProgress) { it(100f) }
            onNodeWithText("18 – 100").assertExists()
        }

    @Test
    fun `A closed card names its running LFO, and one whose effect is off says it waits`() =
        runComposeUiTest {
            panel.set(amp.control("reverb")!!, 127)
            panel.lfos.switch(amp.control("reverb")!!, on = true)
            shown()
            onNodeWithText("LFO · Volume · 0.5 Hz").assertExists()
            onNodeWithTag("fx_lfo_note").assertExists()
            panel.set(amp.control("reverb")!!, 0)
            onNodeWithText("LFO waiting: Reverb is off").assertExists()
        }

    @Test
    fun `Modulation's LFO moves its type or intensity, and a type swing across two effects says so`() =
        runComposeUiTest {
            effectsOn()
            shown()
            onNodeWithTag("effect:modulation").performClick()
            onNodeWithTag("fx_lfo_modulation").performClick()
            assertEquals(
                "modulation_intensity",
                panel.lfos.running
                    .getValue("modulation")
                    .control.id,
            )
            onNodeWithTag("fx_lfo_modulation_target:type").performClick()
            assertEquals(
                "modulation_type",
                panel.lfos.running
                    .getValue("modulation")
                    .control.id,
            )
            onNodeWithText("crosses", substring = true).assertDoesNotExist()
            onNodeWithTag(
                "fx_lfo_modulation_range_high",
            ).performSemanticsAction(SemanticsActions.SetProgress) { it(120f) }
            onNodeWithText("Keep Type inside one effect: a wide sweep crosses from Phaser into Tremolo.").assertExists()
        }

    @Test
    fun `A rate past five a second warns it may sound stepped, and an LFO on the time says the lock is off`() =
        runComposeUiTest {
            effectsOn()
            val delay = amp.control("delay")!!
            panel.lfos.switch(delay, on = true)
            panel.lfos.aim(delay, amp.control("delay_time")!!)
            panel.lfos.set(
                delay,
                panel.lfos.running
                    .getValue("delay")
                    .copy(hz = 6.0),
            )
            shown()
            onNodeWithTag("effect:delay").performClick()
            onNodeWithTag("fx_lfo_delay_hz_value").assertTextEquals("6 Hz")
            onNodeWithText("Above 5 Hz it may sound stepped", substring = true).assertExists()
            onNodeWithText("Tempo lock turned off: the LFO is moving Time.", substring = true).assertExists()
        }
}
