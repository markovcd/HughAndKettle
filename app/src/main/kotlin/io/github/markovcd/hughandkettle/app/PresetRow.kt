package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/**
 * The current preset in large type, as bank and slot where the amp numbers them that way, between a
 * step down and a step up that wrap around. Tests find it by `preset`, `preset_down` and `preset_up`.
 */
@Composable
internal fun PresetRow(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val amp = panel.amp
    val preset = panel.state.preset
    Row(
        modifier = modifier.fillMaxWidth().height(88.dp).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { panel.recall(if (preset == null || preset == 1) amp.presetCount else preset - 1) },
            enabled = enabled,
            modifier = Modifier.size(64.dp).testTag("preset_down"),
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val slot = preset?.let(amp::slotOf)
            Text(
                text = preset?.toString() ?: "–",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.testTag("preset"),
            )
            Text(
                text =
                    if (slot != null) {
                        stringResource(R.string.preset_bank_slot, slot.bank, slot.slot)
                    } else {
                        stringResource(R.string.preset)
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        FilledTonalButton(
            onClick = { panel.recall(if (preset == null || preset == amp.presetCount) 1 else preset + 1) },
            enabled = enabled,
            modifier = Modifier.size(64.dp).testTag("preset_up"),
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
}
