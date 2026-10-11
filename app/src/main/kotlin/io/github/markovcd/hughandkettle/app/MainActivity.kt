package io.github.markovcd.hughandkettle.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import io.github.markovcd.hughandkettle.amp.AmpModels

/** The one window: a panel for the amp, over a link to its WMI-1. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val scope = rememberCoroutineScope()
            val panel = remember { PanelState(AmpModels.blackSpirit200, scope) }
            MaterialTheme {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) {
                    AmpPanel(panel)
                }
            }
        }
    }
}
