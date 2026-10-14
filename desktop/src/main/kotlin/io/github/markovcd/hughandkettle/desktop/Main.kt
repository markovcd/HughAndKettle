package io.github.markovcd.hughandkettle.desktop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import io.github.markovcd.hughandkettle.app.Knob
import io.github.markovcd.hughandkettle.app.theme.HughAndKettleTheme

/** The desktop app: a window over the shared screens. */
fun main() =
    application {
        Window(onCloseRequest = ::exitApplication, title = "Hugh & Kettle") {
            HughAndKettleTheme(dark = true) {
                var gain by remember { mutableIntStateOf(64) }
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Knob(value = gain, onValue = { gain = it }, description = "Gain", size = 120.dp)
                        Text("Gain $gain", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
