package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.AmpModels

/**
 * The settings behind a three-dot button: the amp behind the box and the theme. Tests find it by `settings`
 * and the items by `amp:<id>` and `theme:<id>`.
 */
@Composable
internal fun SettingsMenu(
    panel: PanelState,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    val words = stringResource(R.string.settings)
    Box {
        IconButton(
            onClick = { open = true },
            modifier = Modifier.testTag("settings").semantics { contentDescription = words },
        ) {
            val color = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(Modifier.size(4.dp, 18.dp)) {
                val radius = size.width / 2
                listOf(radius, size.height / 2, size.height - radius).forEach {
                    drawCircle(color, radius, Offset(radius, it))
                }
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            MenuHeading(stringResource(R.string.amp))
            AmpModels.all.forEach { amp ->
                DropdownMenuItem(
                    text = { Text(amp.name) },
                    leadingIcon = { RadioButton(selected = amp == panel.amp, onClick = null) },
                    onClick = {
                        open = false
                        panel.choose(amp)
                    },
                    modifier = Modifier.testTag("amp:${amp.id}"),
                )
            }
            HorizontalDivider()
            MenuHeading(stringResource(R.string.theme))
            ThemeChoice.entries.forEach { choice ->
                DropdownMenuItem(
                    text = { Text(stringResource(choice.label)) },
                    leadingIcon = { RadioButton(selected = choice == theme, onClick = null) },
                    onClick = {
                        open = false
                        onTheme(choice)
                    },
                    modifier = Modifier.testTag("theme:${choice.id}"),
                )
            }
        }
    }
}

@Composable
private fun MenuHeading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
