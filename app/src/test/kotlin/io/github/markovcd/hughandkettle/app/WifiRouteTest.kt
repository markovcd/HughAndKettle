package io.github.markovcd.hughandkettle.app

import android.net.ConnectivityManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetwork
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
}
