package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.ControlReport
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.amp.ValueBlock
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiChannel
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.FakeWmiEvent
import io.github.markovcd.hughandkettle.wmi.WmiAddress
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

@OptIn(ExperimentalTestApi::class)
class WriteSheetTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val folder: Path = Files.createTempDirectory("write")
    private val amp = AmpModels.grandMeisterDeluxe40
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val names = PresetNamesState(PresetNameStore(folder.resolve("names")), panel, scope, Dispatchers.Unconfined)
    private val shelf =
        ToneShelf(
            ToneStore(folder.resolve("tones")),
            panel,
            scope,
            Dispatchers.Unconfined,
            holder.heard,
            names = { names.names },
            named = names::name,
        )
    private var recalls = 0

    // A stand-in amp: the first recall of preset 47 answers with what it holds, the tone's volume is met with
    // STORE pressed, and the second recall, which stores, with the volume a little lower than the tone's.
    private val fake: FakeWmi =
        FakeWmi(onEvent = { event ->
            val message = (event as? FakeWmiEvent.Received)?.message ?: return@FakeWmi
            if (message == ProgramChange(MidiChannel(1), 46)) {
                recalls++
                fake.emit(ValueBlock(if (recalls == 1) mapOf(56 to 60) else mapOf(56 to 200, 57 to 168), 47).message())
            }
            if (message == ControlChange(MidiChannel(1), 57, 90)) fake.emit(ControlReport(80, 127).message())
        })

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
        folder.deleteRecursively()
    }

    @Test
    fun `A tone is written from its menu into the preset picked, and what the amp stored otherwise is shown`() =
        runComposeUiTest {
            panel.set(amp.control("gain")!!, 100)
            panel.set(amp.control("volume")!!, 90)
            shelf.save("Solo")
            setContent { ToneShelfCard(shelf, enabled = true) }
            waitUntil("saved", timeoutMillis = 5_000) { shelf.tones.isNotEmpty() }
            onNodeWithTag("tone_menu:Solo").performClick()
            onNodeWithTag("tone_write").assertIsNotEnabled()
            panel.connect(WmiAddress("127.0.0.1", fake.port))
            waitUntil("linked", timeoutMillis = 5_000) { panel.linked }
            onNodeWithTag("tone_write").performClick()
            onNodeWithTag("write_presets").performScrollToNode(hasTestTag("write_preset:47"))
            onNodeWithTag("write_preset:47").performClick()
            onNodeWithTag("write_confirm").performClick()
            waitUntil("stored", timeoutMillis = 5_000) {
                onAllNodes(hasTestTag("write_result:stored_with_differences")).fetchSemanticsNodes().isNotEmpty()
            }
            onNodeWithTag("write_diffs").assertExists()
            waitUntil("named", timeoutMillis = 5_000) { names.names[47] == "Solo" }
            assertEquals(listOf("Solo", "Backup of Bank 12 · 3"), shelf.tones.map { it.name })
            onNodeWithTag("write_done").performClick()
            onNodeWithTag("write_sheet").assertDoesNotExist()
        }
}
