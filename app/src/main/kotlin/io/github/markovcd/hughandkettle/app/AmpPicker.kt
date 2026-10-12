package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.markovcd.hughandkettle.amp.AmpModels

/** The amp's name, which opens the list of amps to pick from. Tests find it by `amp` and the items by `amp:<id>`. */
@Composable
internal fun AmpPicker(
    panel: PanelState,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(onClick = { open = true }, modifier = Modifier.testTag("amp")) {
            Text(
                "${panel.amp.name} ▾",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            AmpModels.all.forEach { amp ->
                DropdownMenuItem(
                    text = { Text(amp.name) },
                    onClick = {
                        open = false
                        panel.choose(amp)
                    },
                    modifier = Modifier.testTag("amp:${amp.id}"),
                )
            }
        }
    }
}
