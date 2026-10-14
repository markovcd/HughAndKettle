package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.preset
import io.github.markovcd.hughandkettle.ui.preset_bank_slot
import org.jetbrains.compose.resources.stringResource

/**
 * The current preset in large type, under it the name given to it or else its bank and slot where the
 * amp numbers them that way, between a step down and a step up that wrap around. A tap on the preset
 * opens the grid of all of them. Tests find it by `preset`, `preset_label`, `preset_down` and `preset_up`.
 */
@Composable
internal fun PresetRow(
    panel: PanelState,
    names: PresetNamesState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val amp = panel.amp
    val preset = panel.state.preset
    var picking by remember { mutableStateOf(false) }
    Row(
        modifier = modifier.fillMaxWidth().height(72.dp).padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { panel.recall(if (preset == null || preset == 1) amp.presetCount else preset - 1) },
            enabled = enabled,
            modifier = Modifier.size(56.dp).testTag("preset_down"),
        ) { Text("−", style = MaterialTheme.typography.headlineSmall) }
        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            val slot = preset?.let(amp::slotOf)
            val name = preset?.let { names.names[it] }
            Text(
                text = preset?.toString() ?: "–",
                style = MaterialTheme.typography.displaySmall,
                modifier = Modifier.clickable(enabled = enabled) { picking = true }.testTag("preset"),
            )
            Text(
                text =
                    if (name != null) {
                        name
                    } else if (slot != null) {
                        stringResource(Res.string.preset_bank_slot, slot.bank, slot.slot)
                    } else {
                        stringResource(Res.string.preset)
                    },
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("preset_label"),
            )
        }
        FilledTonalButton(
            onClick = { panel.recall(if (preset == null || preset == amp.presetCount) 1 else preset + 1) },
            enabled = enabled,
            modifier = Modifier.size(56.dp).testTag("preset_up"),
        ) { Text("+", style = MaterialTheme.typography.headlineSmall) }
    }
    if (picking) PresetGrid(panel, names, onClose = { picking = false })
}
