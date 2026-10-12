package io.github.markovcd.hughandkettle.app

import androidx.test.core.app.ApplicationProvider
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class AmpChoiceStoreTest {
    private val app = ApplicationProvider.getApplicationContext<HughAndKettleApplication>()

    @Test
    fun `The amp picked for each box reads back, and a later pick for a box replaces the earlier`() {
        val home = WmiAddress("192.168.1.1", 2000)
        val rehearsal = WmiAddress("10.0.0.7", 2000)
        runBlocking {
            app.amps.remember(home, AmpModels.blackSpirit200)
            app.amps.remember(rehearsal, AmpModels.blackSpirit200)
            app.amps.remember(home, AmpModels.grandMeisterDeluxe40)
        }
        assertEquals(
            mapOf(home to AmpModels.grandMeisterDeluxe40, rehearsal to AmpModels.blackSpirit200),
            runBlocking { app.amps.choices.first() },
        )
    }
}
