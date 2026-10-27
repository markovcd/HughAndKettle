package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.LoudnessCorrectionStore
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.ToneStore
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
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
import org.robolectric.annotation.Config
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AmpPanelTest {
    @get:Rule
    val compose = createComposeRule()

    private val folder: Path = Files.createTempDirectory("panel")
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val panel = PanelState(AmpModels.blackSpirit200, holder, scope, flowOf(null))
    private val shelf = ToneShelf(ToneStore(folder.resolve("tones")), panel, scope, Dispatchers.Unconfined)
    private val presetNames =
        PresetNamesState(PresetNameStore(folder.resolve("preset-names")), panel, scope, Dispatchers.Unconfined)
    private val songs = SongState(SongStore(folder.resolve("songs")), panel, scope, Dispatchers.Unconfined)
    private val match =
        MatchState(
            LoudnessCorrectionStore(folder.resolve("loudness")),
            panel,
            scope,
            { presetNames.names },
            { songs.song },
            Dispatchers.Unconfined,
        )

    @After
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private var picked: ThemeChoice? = null

    private fun shown(mode: SwitcherMode = SwitcherMode.Presets) =
        compose.setContent {
            AmpPanel(
                panel,
                DiscoveryState(WmiDiscovery(0)),
                shelf,
                presetNames,
                songs,
                match,
                ThemeChoice.Dark,
                onTheme = { picked = it },
                mode,
                onMode = {},
            )
        }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `A phone shows the amp's tab, and the effects only on their own tab`() {
        shown()
        compose.onNodeWithTag("control:gain").assertExists()
        compose.onNodeWithTag("control:delay").assertDoesNotExist()
        compose.onNodeWithTag("tab:fx").performClick()
        compose.onNodeWithTag("control:delay").assertExists()
        compose.onNodeWithTag("control:gain").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w915dp-h412dp-land")
    fun `In landscape the effects get a pane of their own beside the amp`() {
        shown()
        compose.onNode(hasTestTag("control:gain") and hasAnyAncestor(hasTestTag("panel"))).assertExists()
        compose.onNode(hasTestTag("control:delay") and hasAnyAncestor(hasTestTag("controls"))).assertExists()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `Picking another amp in the header's menu puts its controls on the panel`() {
        shown()
        compose.onNodeWithTag("header_menu").performClick()
        compose.onNodeWithTag("menu_amp").performClick()
        compose.onNodeWithTag("amp:grandmeister_deluxe_40").performClick()
        compose.onNodeWithTag("control:power_soak").performScrollTo().assertExists()
        assertEquals(AmpModels.grandMeisterDeluxe40, panel.amp)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `The amp picked in answer to the question goes on the panel and puts the question away`() {
        shown()
        compose.onNodeWithTag("amp_question").assertDoesNotExist()
        val box = FakeWmi(onEvent = {})
        try {
            panel.connect(WmiAddress("127.0.0.1", box.port))
            compose.waitUntil(5_000) { panel.asking }
            compose.onNodeWithTag("amp_answer:grandmeister_deluxe_40").performClick()
            compose.onNodeWithTag("amp_question").assertDoesNotExist()
            assertEquals(AmpModels.grandMeisterDeluxe40, panel.amp)
        } finally {
            panel.disconnect()
            box.close()
        }
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `The theme is picked in the header's menu`() {
        shown()
        compose.onNodeWithTag("header_menu").performClick()
        compose.onNodeWithTag("theme:light").performClick()
        assertEquals(ThemeChoice.Light, picked)
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `The link card sits on the panel until a link starts, and the header's menu ends the link`() {
        val fake = FakeWmi(onEvent = {})
        shown()
        compose.onNodeWithTag("enter_address").assertExists()
        panel.connect(WmiAddress("127.0.0.1", fake.port))
        compose.waitUntil(5_000) { panel.linked }
        panel.settle()
        compose.onNodeWithTag("enter_address").assertDoesNotExist()
        compose.onNodeWithTag("header_menu").performClick()
        compose.onNodeWithTag("menu_connect").performClick()
        compose.waitUntil(5_000) { panel.idle }
        compose.onNodeWithTag("link_dialog").assertDoesNotExist()
        compose.onNodeWithTag("enter_address").assertExists()
        fake.close()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `With no tone pinned, a tap on the header in the Tones mode goes to the Tones tab`() {
        shown(SwitcherMode.Tones)
        compose.onNodeWithTag("tone_save").assertDoesNotExist()
        compose.onNodeWithTag("switcher_pick").performClick()
        compose.onNodeWithTag("tone_save").assertExists()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `Switching an effect on opens its card and closes the others, and switching it off hides its controls`() {
        val fake = FakeWmi(onEvent = {})
        panel.connect(WmiAddress("127.0.0.1", fake.port))
        compose.waitUntil(5_000) { panel.linked }
        panel.settle()
        shown()
        compose.onNodeWithTag("tab:fx").performClick()
        compose.onNodeWithTag("control:delay_time").assertDoesNotExist()
        compose.onNodeWithTag("control:delay").performClick()
        compose.onNodeWithTag("control:delay_time").assertExists()
        compose.onNodeWithTag("control:modulation").performClick()
        compose.onNodeWithTag("control:modulation_speed").assertExists()
        compose.onNodeWithTag("control:delay_time").assertDoesNotExist()
        compose.onNodeWithTag("effect:delay").performClick()
        compose.onNodeWithTag("control:delay_time").assertExists()
        compose.onNodeWithTag("control:delay").performClick()
        compose.onNodeWithTag("control:delay_time").assertDoesNotExist()
        fake.close()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `The noise gate's sensitivity shows under its switch only while the gate is on`() {
        val fake = FakeWmi(onEvent = {})
        panel.connect(WmiAddress("127.0.0.1", fake.port))
        compose.waitUntil(5_000) { panel.linked }
        panel.settle()
        shown()
        compose.onNodeWithTag("control:noise_gate_sensitivity").assertDoesNotExist()
        compose.onNodeWithTag("control:noise_gate").performScrollTo().performClick()
        compose.onNodeWithTag("control:noise_gate_sensitivity").assertExists()
        compose.onNodeWithTag("control:noise_gate").performClick()
        compose.onNodeWithTag("control:noise_gate_sensitivity").assertDoesNotExist()
        fake.close()
    }

    @Test
    @Config(qualifiers = "w412dp-h915dp")
    fun `The song has a tab of its own, where a step is added and Play waits for a link`() {
        shown()
        compose.onNodeWithTag("tab:song").performClick()
        compose.onNodeWithTag("song_play").assertIsNotEnabled()
        compose.onNodeWithTag("song_add").performScrollTo().performClick()
        compose.onNodeWithTag("song_step:1").assertExists()
        compose.onNodeWithTag("song_beats_up:1").performScrollTo().performClick()
        assertEquals(
            20,
            songs.song.steps
                .single()
                .beats,
        )
        compose.onNodeWithTag("song_play").assertIsNotEnabled()
        compose.onNodeWithTag("song_tempo_up").performScrollTo().performClick()
        compose.onNodeWithTag("song_tempo").assertTextEquals("121")
        compose
            .onNodeWithTag("song_count_in")
            .performScrollTo()
            .assertIsOn()
            .performClick()
            .assertIsOff()
    }
}
