package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Sectors

/** The whole screen: where the WMI-1 is, then one row per control. Tests find rows by `control:<id>`. */
@Composable
fun AmpPanel(panel: PanelState) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(panel.amp.name, style = MaterialTheme.typography.titleLarge)
        LinkRow(panel)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(panel.amp.controls, key = Control::id) { control ->
                ControlRow(
                    control = control,
                    value = panel.values[control.id] ?: 0,
                    onValue = { panel.set(control, it) },
                    modifier = Modifier.testTag("control:${control.id}"),
                )
            }
        }
    }
}

@Composable
private fun LinkRow(panel: PanelState) {
    val link = panel.link
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = panel.host,
            onValueChange = { panel.host = it },
            label = { Text(stringResource(R.string.host)) },
            singleLine = true,
            enabled = link == null,
            modifier = Modifier.weight(2f).testTag("host"),
        )
        OutlinedTextField(
            value = panel.port,
            onValueChange = { panel.port = it },
            label = { Text(stringResource(R.string.port)) },
            singleLine = true,
            enabled = link == null,
            modifier = Modifier.weight(1f).testTag("port"),
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (link == null) {
            Button(
                onClick = panel::connect,
                modifier = Modifier.testTag("connect"),
            ) { Text(stringResource(R.string.connect)) }
        } else {
            Button(onClick = panel::disconnect, modifier = Modifier.testTag("disconnect")) {
                Text(stringResource(R.string.disconnect))
            }
        }
        Text(
            text =
                panel.failure
                    ?: link?.let { stringResource(R.string.connected_to, it.remote.toString()) }
                    ?: stringResource(R.string.not_connected),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.testTag("status"),
        )
    }
}

@Composable
private fun ControlRow(
    control: Control,
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        Text(control.name, style = MaterialTheme.typography.labelLarge)
        when (val kind = control.kind) {
            ControlKind.Knob -> {
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onValue(it.toInt()) },
                    valueRange = 0f..127f,
                )
            }

            ControlKind.Switch -> {
                Switch(checked = value >= 64, onCheckedChange = { onValue(if (it) 127 else 0) })
            }

            is ControlKind.Selector -> {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    kind.options.forEachIndexed { index, option ->
                        FilterChip(
                            selected = Sectors.index(value, kind.options.size) == index,
                            onClick = { onValue(Sectors.value(index, kind.options.size)) },
                            label = { Text(option) },
                        )
                    }
                }
            }
        }
    }
}
