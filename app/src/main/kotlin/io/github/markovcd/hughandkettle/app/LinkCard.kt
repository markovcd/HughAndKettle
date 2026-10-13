package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/**
 * A status line, then, while there is no link, the USB MIDI cables plugged in, the WMI-1s heard on the
 * network, and address and port behind "Enter address" for a box discovery does not find. A box heard
 * is [offered][PanelState.offered] to the panel, which may link to it by itself.
 */
@Composable
internal fun LinkCard(
    panel: PanelState,
    discovery: DiscoveryState,
    modifier: Modifier = Modifier,
) {
    var typing by rememberSaveable { mutableStateOf(false) }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            StatusLine(panel)
            if (panel.idle) {
                FoundCables(panel.cables, onPick = panel::connect)
                FoundWmis(discovery, panel.last, onPick = panel::connect)
                LaunchedEffect(discovery.found) { panel.offered(discovery.found.map { it.wmiAddress() }) }
                if (typing) {
                    AddressFields(panel)
                    Button(
                        onClick = panel::connect,
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("connect"),
                    ) {
                        Text(stringResource(R.string.connect))
                    }
                } else {
                    TextButton(onClick = { typing = true }, modifier = Modifier.testTag("enter_address")) {
                        Text(stringResource(R.string.enter_address))
                    }
                }
            } else {
                OutlinedButton(
                    onClick = panel::disconnect,
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("disconnect"),
                ) {
                    Text(stringResource(R.string.disconnect))
                }
            }
        }
    }
}

@Composable
private fun StatusLine(panel: PanelState) {
    val failure = panel.failure
    val colors = MaterialTheme.colorScheme
    val warning = panel.warning
    val band = if (warning) Modifier.background(colors.errorContainer, RoundedCornerShape(8.dp)) else Modifier
    val resources = LocalResources.current
    val text =
        if (failure !=
            null
        ) {
            LinkWords.of(failure, resources)
        } else {
            LinkWords.of(panel.link, panel.cable, resources)
        }
    Row(
        modifier = band.defaultMinSize(minHeight = 36.dp).padding(horizontal = 10.dp).fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LinkGlyph(panel)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (warning) colors.onErrorContainer else colors.onSurface,
            modifier = Modifier.testTag("status"),
        )
    }
}

@Composable
private fun AddressFields(panel: PanelState) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
        OutlinedTextField(
            value = panel.host,
            onValueChange = { panel.host = it },
            label = { Text(stringResource(R.string.host)) },
            singleLine = true,
            enabled = panel.idle,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.weight(1f).testTag("host"),
        )
        OutlinedTextField(
            value = panel.port,
            onValueChange = { panel.port = it },
            label = { Text(stringResource(R.string.port)) },
            singleLine = true,
            enabled = panel.idle,
            textStyle = MaterialTheme.typography.bodyLarge,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.width(96.dp).testTag("port"),
        )
    }
}
