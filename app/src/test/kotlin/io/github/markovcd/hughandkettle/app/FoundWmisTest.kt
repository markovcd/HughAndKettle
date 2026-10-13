package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDiscovery
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds

@RunWith(RobolectricTestRunner::class)
class FoundWmisTest {
    @get:Rule
    val compose = createComposeRule()

    private val fake = FakeWmi(deviceId = "WMI-1_A1B2C3", onEvent = {})

    @After
    fun teardown() = fake.close()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `A broadcasting WMI-1 is listed, and tapping it picks its address`() {
        val port = DatagramSocket(0).use { it.localPort }
        fake.broadcast(InetAddress.getLoopbackAddress(), port, interval = 100.milliseconds)
        var picked: WmiAddress? = null
        compose.setContent { FoundWmis(DiscoveryState(WmiDiscovery(port)), last = null, onPick = { picked = it }) }
        // Capped at 5 s so a broken broadcaster fails rather than hangs; the first broadcast ends it.
        compose.waitUntilExactlyOneExists(hasTestTag("found:WMI-1_A1B2C3"), timeoutMillis = 5_000)
        compose.onNodeWithTag("found:WMI-1_A1B2C3").performClick()
        compose.waitForIdle()
        assertEquals(WmiAddress("127.0.0.1", fake.port), picked)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `A WMI-1 broadcasting to any of the ports listened on is listed`() {
        val quiet = DatagramSocket(0).use { it.localPort }
        val port = DatagramSocket(0).use { it.localPort }
        fake.broadcast(InetAddress.getLoopbackAddress(), port, interval = 100.milliseconds)
        val discovery = DiscoveryState(listOf(WmiDiscovery(quiet), WmiDiscovery(port)))
        compose.setContent { FoundWmis(discovery, last = null, onPick = {}) }
        // Capped at 5 s so a broken broadcaster fails rather than hangs; the first broadcast ends it.
        compose.waitUntilExactlyOneExists(hasTestTag("found:WMI-1_A1B2C3"), timeoutMillis = 5_000)
    }

    @Test
    fun `The last box used is offered first, ahead of anything heard`() {
        val last = WmiAddress("192.168.1.20", 2000)
        var picked: WmiAddress? = null
        compose.setContent { FoundWmis(DiscoveryState(WmiDiscovery(0)), last, onPick = { picked = it }) }
        compose.onNodeWithTag("last").performClick()
        compose.waitForIdle()
        assertEquals(last, picked)
    }
}
