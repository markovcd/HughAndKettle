package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.wmi.FakeWmi
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
class SwitcherRowTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val folder: Path = Files.createTempDirectory("switcher")
    private val fake = FakeWmi(onEvent = {})
    private val amp = AmpModels.grandMeisterDeluxe40
    private val panel = PanelState(amp, holder, scope, flowOf(null))
    private val names = PresetNamesState(PresetNameStore(folder.resolve("names")), panel, scope, Dispatchers.Unconfined)
    private val shelf = ToneShelf(ToneStore(folder.resolve("tones")), panel, scope, Dispatchers.Unconfined)
    private val songs = SongState(SongStore(folder.resolve("songs")), panel, scope, Dispatchers.Unconfined)
    private val mute = amp.control("mute")!!
    private var toTones = 0
    private val modes = mutableListOf<SwitcherMode>()

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        fake.close()
        folder.deleteRecursively()
    }

    private fun ComposeUiTest.shown(
        mode: SwitcherMode = SwitcherMode.Presets,
        enabled: Boolean = true,
    ) = setContent {
        SwitcherRow(panel, DiscoveryState(), names, shelf, songs, mode, enabled, onTones = { toTones++ }, menu = {
            close,
            link,
            ->
            HeaderMenu(panel, mode, { modes += it }, ThemeChoice.Dark, {}, close, link, onMatch = null)
        })
    }

    /** Saves a tone for each of [tones], pinning those [pinned] names. */
    private fun ComposeUiTest.tones(
        vararg tones: String,
        pinned: Set<String> = tones.toSet(),
    ) {
        tones.forEachIndexed { i, name ->
            panel.set(amp.control("gain")!!, i + 1)
            shelf.save(name)
            waitUntil("saved $name", timeoutMillis = 5_000) { shelf.tones.size == i + 1 }
        }
        shelf.tones.filter { it.name in pinned }.forEach { shelf.pin(it, on = true) }
        waitUntil("pinned", timeoutMillis = 5_000) { shelf.pinned.size == pinned.size }
    }

    private fun ComposeUiTest.linked() {
        panel.connect(WmiAddress("127.0.0.1", fake.port))
        waitUntil("linked", timeoutMillis = 5_000) { panel.linked }
    }

    @Test
    fun `The minus and plus step the preset, wrapping from none known to the last and from the last to the first`() =
        runComposeUiTest {
            shown()
            onNodeWithTag("switcher_prev").performClick()
            assertEquals(128, panel.state.preset)
            onNodeWithTag("switcher_next").performClick()
            assertEquals(1, panel.state.preset)
        }

    @Test
    fun `A tap on the preset opens the picker by banks, and a preset tapped there is recalled and closes it`() =
        runComposeUiTest {
            panel.recall(6)
            shown()
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("preset_bank:2").assertExists()
            onNodeWithTag("preset_picker_list").performScrollToNode(hasTestTag("preset_pick:128"))
            onNodeWithTag("preset_pick:128").performClick()
            assertEquals(128, panel.state.preset)
            onNodeWithTag("preset_picker").assertDoesNotExist()
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("preset_picker_close").performClick()
            onNodeWithTag("preset_picker").assertDoesNotExist()
        }

    @Test
    fun `A header that is off neither steps nor opens the picker`() =
        runComposeUiTest {
            shown(enabled = false)
            onNodeWithTag("switcher_next").assertIsNotEnabled()
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("preset_picker").assertDoesNotExist()
        }

    @Test
    fun `A preset held in the picker is named, and its name shows in the header`() =
        runComposeUiTest {
            panel.recall(1)
            shown()
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("preset_pick:1").performTouchInput { longClick() }
            onNodeWithTag("tone_name").performTextReplacement("Lead solo")
            onNodeWithTag("tone_name_done").performClick()
            waitUntil(timeoutMillis = 5_000) { names.names[1] == "Lead solo" }
            onNodeWithTag("preset_picker_close").performClick()
            onNode(
                hasText("Lead solo", substring = true) and hasAnyAncestor(hasTestTag("switcher_line")),
                useUnmergedTree = true,
            ).assertExists()
            assertEquals("Lead solo", PresetNameStore(folder.resolve("names")).names(amp).getOrThrow()[1])
        }

    @Test
    fun `With no tone pinned, the minus and plus are off and the middle goes to the Tones tab`() =
        runComposeUiTest {
            tones("One", pinned = emptySet())
            shown(SwitcherMode.Tones)
            onNodeWithTag("switcher_next").assertIsNotEnabled()
            onNodeWithTag("switcher_pick").performClick()
            assertEquals(1, toTones)
        }

    @Test
    fun `In the Tones mode the plus loads the first pinned tone and the caption counts among the pinned`() =
        runComposeUiTest {
            tones("One", "Two", "Three", pinned = setOf("One", "Three"))
            shown(SwitcherMode.Tones)
            onNodeWithTag("switcher_caption", useUnmergedTree = true).assertTextEquals("TONE · 2 PINNED")
            onNodeWithTag("switcher_next").assertIsEnabled().performClick()
            assertEquals("One", shelf.recalled)
            onNodeWithTag("switcher_caption", useUnmergedTree = true).assertTextEquals("TONE · 1 OF 2")
            onNodeWithTag("switcher_prev").performClick()
            assertEquals("Three", shelf.recalled)
        }

    @Test
    fun `The tone picker lists the pinned tones alone, loads the one tapped, and its link goes to the Tones tab`() =
        runComposeUiTest {
            tones("One", "Two", "Three", pinned = setOf("One", "Three"))
            shown(SwitcherMode.Tones)
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("tone_pick:Two").assertDoesNotExist()
            onNodeWithTag("tone_pick:Three").performClick()
            assertEquals("Three", shelf.recalled)
            onNodeWithTag("tone_picker").assertDoesNotExist()
            onNodeWithTag("switcher_pick").performClick()
            onNodeWithTag("tone_picker_manage").performClick()
            onNodeWithTag("tone_picker").assertDoesNotExist()
            assertEquals(1, toTones)
        }

    @Test
    fun `The menu picks the amp from its list and what the minus and plus step through`() =
        runComposeUiTest {
            shown()
            onNodeWithTag("header_menu").performClick()
            onNodeWithTag("switcher_mode:tones").performClick()
            assertEquals(listOf(SwitcherMode.Tones), modes)
            onNodeWithTag("menu_amp").performClick()
            onNodeWithTag("amp:black_spirit_200").performClick()
            assertEquals(AmpModels.blackSpirit200, panel.amp)
        }

    @Test
    fun `Offline the chip opens the menu, linked the menu mutes and unmutes and disconnects`() =
        runComposeUiTest {
            shown()
            onNodeWithTag("status_offline").performClick()
            onNodeWithTag("menu_amp").assertExists()
            linked()
            onNodeWithTag("status_offline").assertDoesNotExist()
            onNodeWithTag("status_linked").assertExists()
            onNodeWithTag("menu_mute").performClick()
            waitUntil("muted", timeoutMillis = 5_000) { mute.isOn(panel.state.values[mute.id] ?: 0) }
            onNodeWithTag("menu_mute").performClick()
            waitUntil("unmuted", timeoutMillis = 5_000) { panel.state.values[mute.id] == 0 }
            onNodeWithTag("menu_connect").performClick()
            waitUntil("idle", timeoutMillis = 5_000) { panel.idle }
        }

    @Test
    fun `Muted, the menu's corner shows the mute mark in place of the linked dot, and no chip`() =
        runComposeUiTest {
            shown()
            linked()
            panel.set(mute, 127)
            onNode(hasTestTag("status_muted") and hasContentDescription("Muted")).assertExists()
            onNode(hasTestTag("status_muted") and hasText("Muted")).assertDoesNotExist()
            onNodeWithTag("status_linked").assertDoesNotExist()
            panel.set(mute, 0)
            onNodeWithTag("status_muted").assertDoesNotExist()
            onNodeWithTag("status_linked").assertExists()
        }
}
