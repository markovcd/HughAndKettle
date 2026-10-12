package io.github.markovcd.hughandkettle.app

import android.content.ComponentName
import android.content.Context
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.app.theme.Palette
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals

@RunWith(RobolectricTestRunner::class)
class SplashThemeTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private fun launchTheme(): Int =
        context.packageManager.getActivityInfo(ComponentName(context, MainActivity::class.java), 0).theme

    private fun resourceOf(
        style: Int,
        attribute: Int,
    ): Int {
        val theme = context.resources.newTheme().apply { applyStyle(style, true) }
        val values = theme.obtainStyledAttributes(intArrayOf(attribute))
        return values.getResourceId(0, 0).also { values.recycle() }
    }

    @Test
    fun `The app launches into a splash of the icon's mark on the stage surface`() {
        assertEquals(R.style.Theme_HughAndKettle_Starting, launchTheme())
        assertEquals(R.color.stage, resourceOf(launchTheme(), android.R.attr.windowSplashScreenBackground))
        assertEquals(
            R.drawable.ic_launcher_foreground,
            resourceOf(launchTheme(), android.R.attr.windowSplashScreenAnimatedIcon),
        )
    }

    @Test
    fun `The stage behind the splash is the dark scheme's surface`() {
        assertEquals(Palette.Dark.surface.toArgb(), context.getColor(R.color.stage))
    }

    @Test
    fun `The panel's window wears the stage without the mark`() {
        assertEquals(R.color.stage, resourceOf(R.style.Theme_HughAndKettle, android.R.attr.windowBackground))
    }
}
