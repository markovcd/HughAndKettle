package io.github.markovcd.hughandkettle.app

import android.net.ConnectivityManager
import android.net.wifi.WifiInfo
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
import org.robolectric.shadows.ShadowNetworkCapabilities
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
class WifiRouteTest {
    private val connectivity =
        ApplicationProvider.getApplicationContext<HughAndKettleApplication>().getSystemService(
            ConnectivityManager::class.java,
        )

    @Test
    fun `The app's sockets follow the WiFi network while it is up`() {
        connectivity.bindProcessToNetwork(null)
        WifiRoute(connectivity).start()
        val wifi = ShadowNetwork.newInstance(7)
        val callbacks = shadowOf(connectivity).networkCallbacks
        callbacks.forEach { it.onAvailable(wifi) }
        assertEquals(wifi, connectivity.boundNetworkForProcess)
        callbacks.forEach { it.onLost(wifi) }
        assertNull(connectivity.boundNetworkForProcess)
    }

    @Test
    fun `The network the phone is on is named while it is up, and off once it is lost`() {
        connectivity.bindProcessToNetwork(null)
        val route = WifiRoute(connectivity).apply { start() }
        val wifi = ShadowNetwork.newInstance(8)
        val capabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(capabilities).setTransportInfo(WifiInfo.Builder().setSsid("GrandMeister 40".toByteArray()).build())
        val callbacks = shadowOf(connectivity).networkCallbacks
        callbacks.forEach { it.onAvailable(wifi) }
        assertEquals(PhoneWifi.Unnamed, route.network.value)
        callbacks.forEach { it.onCapabilitiesChanged(wifi, capabilities) }
        assertEquals(PhoneWifi.Named("GrandMeister 40"), route.network.value)
        callbacks.forEach { it.onLost(wifi) }
        assertEquals(PhoneWifi.Off, route.network.value)
    }
}
