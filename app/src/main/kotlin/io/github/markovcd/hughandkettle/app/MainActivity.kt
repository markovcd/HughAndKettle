package io.github.markovcd.hughandkettle.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.theme.HughAndKettleTheme
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.launch

/** The one window: a panel for the amp, over a link to its WMI-1. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // The manifest's launch theme carries the splash; the panel's window wears the plain one.
        setTheme(R.style.Theme_HughAndKettle)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val scope = rememberCoroutineScope()
            val app = application as HughAndKettleApplication
            val panel =
                remember {
                    PanelState(
                        AmpModels.all.first(),
                        app.link,
                        scope,
                        app.lastWmi.last,
                        app.amps.choices,
                        app.amps::remember,
                    )
                }
            val discovery = remember { DiscoveryState() }
            val shelf = remember { ToneShelf(app.tones, panel, scope) }
            val theme by app.theme.choice.collectAsState(initial = ThemeChoice.Dark)
            AskForNotifications(linked = panel.link != LinkState.Idle)
            HughAndKettleTheme(
                dark =
                    theme == ThemeChoice.Dark || (theme == ThemeChoice.System && isSystemInDarkTheme()),
            ) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AmpPanel(panel, discovery, shelf, theme, onTheme = { scope.launch { app.theme.pick(it) } })
                }
            }
        }
    }

    /** The service's notification is the only sign of a held link with the screen off, so ask for it on the first link. */
    @Composable
    private fun AskForNotifications(linked: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(linked) {
            val granted =
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (linked && !granted) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
