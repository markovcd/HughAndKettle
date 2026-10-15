package io.github.markovcd.hughandkettle.app

import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@RunWith(RobolectricTestRunner::class)
class LastWmiStoreTest {
    private val fake = FakeWmi(onEvent = {})
    private val app = ApplicationProvider.getApplicationContext<HughAndKettleApplication>()

    @After
    fun teardown() {
        app.link.stop()
        fake.close()
    }

    private fun last(): WmiAddress? =
        runBlocking {
            app.home.lastWmi.last
                .first()
        }

    @Test
    fun `A box reached over a link is remembered`() {
        val address = WmiAddress("127.0.0.1", fake.port)
        app.link.connect(address)
        val started = TimeSource.Monotonic.markNow()
        while (last() != address) {
            if (started.elapsedNow() > 5.seconds) fail("never remembered $address (link is ${app.link.state.value})")
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(5)
        }
    }
}
