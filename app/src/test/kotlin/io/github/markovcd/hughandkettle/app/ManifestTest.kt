package io.github.markovcd.hughandkettle.app

import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
class ManifestTest {
    @Test
    fun `The app asks for the local network the WMI-1 sits on`() {
        val app = ApplicationProvider.getApplicationContext<HughAndKettleApplication>()
        val asked =
            app.packageManager
                .getPackageInfo(
                    app.packageName,
                    PackageManager.GET_PERMISSIONS,
                ).requestedPermissions
                .orEmpty()
        assertTrue("android.permission.ACCESS_LOCAL_NETWORK" in asked, asked.joinToString())
    }
}
