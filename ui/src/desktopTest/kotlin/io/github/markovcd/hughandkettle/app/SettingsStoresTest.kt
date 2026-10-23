package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.writeText
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SettingsStoresTest {
    private val folder: Path = Files.createTempDirectory("settings")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val settings = settingsIn(folder, scope)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        folder.deleteRecursively()
    }

    @Test
    fun `A machine that never picked wears dark, and a picked theme reads back`() {
        val store = ThemeStore(settings)
        assertEquals(ThemeChoice.Dark, runBlocking { store.choice.first() })
        runBlocking { store.pick(ThemeChoice.Light) }
        assertEquals(ThemeChoice.Light, runBlocking { store.choice.first() })
    }

    @Test
    fun `A machine that never picked steps through presets, and a picked mode reads back`() {
        val store = SwitcherModeStore(settings)
        assertEquals(SwitcherMode.Presets, runBlocking { store.mode.first() })
        runBlocking { store.pick(SwitcherMode.Tones) }
        assertEquals(SwitcherMode.Tones, runBlocking { store.mode.first() })
    }

    @Test
    fun `The amp picked for each box reads back, and a later pick for a box replaces the earlier`() {
        val store = AmpChoiceStore(settings)
        val home = WmiAddress("192.168.1.1", 2000)
        val rehearsal = WmiAddress("10.0.0.7", 2000)
        runBlocking {
            store.remember(home, AmpModels.blackSpirit200)
            store.remember(rehearsal, AmpModels.blackSpirit200)
            store.remember(home, AmpModels.grandMeisterDeluxe40)
        }
        assertEquals(
            mapOf(home to AmpModels.grandMeisterDeluxe40, rehearsal to AmpModels.blackSpirit200),
            runBlocking { store.choices.first() },
        )
    }

    @Test
    fun `A remembered box reads back`() {
        val store = LastWmiStore(settings)
        runBlocking { store.remember(WmiAddress("192.168.1.20", 2001)) }
        assertEquals(WmiAddress("192.168.1.20", 2001), runBlocking { store.last.first() })
    }

    @Test
    fun `A damaged settings file reads as no settings rather than stopping the app`() {
        folder.resolve("settings.preferences_pb").writeText("not a settings file")
        assertEquals(ThemeChoice.Dark, runBlocking { ThemeStore(settings).choice.first() })
        assertEquals(SwitcherMode.Presets, runBlocking { SwitcherModeStore(settings).mode.first() })
        assertNull(runBlocking { LastWmiStore(settings).last.first() })
    }
}
