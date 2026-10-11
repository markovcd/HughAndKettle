package io.github.markovcd.hughandkettle.app

import android.app.Notification
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@RunWith(RobolectricTestRunner::class)
class LinkServiceTest {
    private val fake = FakeWmi(onEvent = {})
    private val app = ApplicationProvider.getApplicationContext<HughAndKettleApplication>()

    @After
    fun teardown() {
        app.link.stop()
        fake.close()
    }

    /** Runs the main looper until [holds], for up to 5 s. */
    private fun until(
        what: String,
        holds: () -> Boolean,
    ) {
        val started = TimeSource.Monotonic.markNow()
        while (!holds()) {
            if (started.elapsedNow() > 5.seconds) fail("never got to: $what (link is ${app.link.state.value})")
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }

    @Test
    fun `A held link shows a notification saying where it stands, and the service ends when the link stops`() {
        val address = WmiAddress("127.0.0.1", fake.port)
        app.link.connect(address)
        until("connected") { app.link.state.value == LinkState.Connected(address) }
        val controller = Robolectric.buildService(LinkService::class.java).create().startCommand(0, 1)
        val service = shadowOf(controller.get())
        until("the notification names the box") {
            service.lastForegroundNotification
                ?.extras
                ?.getString(Notification.EXTRA_TEXT) == "Connected to $address"
        }
        app.link.stop()
        until("the service stopped itself") { service.isStoppedBySelf }
        assertTrue(service.isForegroundStopped)
        assertEquals(LinkState.Idle, app.link.state.value)
    }
}
