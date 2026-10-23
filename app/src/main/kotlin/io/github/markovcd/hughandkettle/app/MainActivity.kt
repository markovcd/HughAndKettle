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
                        app.home.lastWmi.last,
                        app.home.amps.choices,
                        app.home.amps::remember,
                    )
                }
            val discovery = remember { DiscoveryState(wifi = app.wifi.network, rereadWifi = app.wifi::reread) }
            val presetNames = remember { PresetNamesState(app.home.presetNames, panel, scope) }
            val (songs, shelf) =
                remember {
                    lateinit var songs: SongState
                    val shelf =
                        ToneShelf(
                            app.home.tones,
                            panel,
                            scope,
                            heard = app.link.heard,
                            names = { presetNames.names },
                            renamed = { tone, to -> songs.renameTone(tone, to) },
                        )
                    // The shelf hands renames to the songs, and the songs look tones up on the shelf.
                    songs =
                        SongState(
                            app.home.songs,
                            panel,
                            scope,
                            click = app.click::play,
                            lag = app.click::lag,
                            tones = { shelf.tones },
                        )
                    songs to shelf
                }
            val theme by app.home.theme.choice
                .collectAsState(initial = ThemeChoice.Dark)
            val mode by app.home.switcher.mode
                .collectAsState(initial = SwitcherMode.Presets)
            AskForLocalNetwork()
            AskForNotifications(linked = !panel.idle)
            HughAndKettleTheme(
                dark =
                    theme == ThemeChoice.Dark || (theme == ThemeChoice.System && isSystemInDarkTheme()),
            ) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AmpPanel(
                        panel,
                        discovery,
                        shelf,
                        presetNames,
                        songs,
                        theme,
                        onTheme = { scope.launch { app.home.theme.pick(it) } },
                        mode,
                        onMode = { scope.launch { app.home.switcher.pick(it) } },
                    )
                }
            }
        }
    }

    /** Without the grant, Android 17 silently drops every packet to and from the WMI-1, so ask before anything else. */
    @Composable
    private fun AskForLocalNetwork() {
        if (Build.VERSION.SDK_INT < LOCAL_NETWORK_GRANTED_SINCE) return
        val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
        LaunchedEffect(Unit) {
            val granted =
                checkSelfPermission(Manifest.permission.ACCESS_LOCAL_NETWORK) == PackageManager.PERMISSION_GRANTED
            if (!granted) ask.launch(Manifest.permission.ACCESS_LOCAL_NETWORK)
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

    private companion object {
        // Android 17, the first release that gates the local network behind a grant.
        const val LOCAL_NETWORK_GRANTED_SINCE = 37
    }
}
