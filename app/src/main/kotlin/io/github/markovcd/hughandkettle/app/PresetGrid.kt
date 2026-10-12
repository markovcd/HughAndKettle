package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/**
 * Every preset of the panel's amp, eight to a row, the current one filled. A tap recalls it and
 * closes the grid. Tests find the grid by `preset_grid` and each preset by `preset:<n>`.
 */
@Composable
internal fun PresetGrid(
    panel: PanelState,
    onClose: () -> Unit,
) {
    val amp = panel.amp
    val current = panel.state.preset
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(R.string.preset_pick)) },
        text = {
            LazyVerticalGrid(
                columns = GridCells.Fixed(PER_ROW),
                state = rememberLazyGridState(initialFirstVisibleItemIndex = ((current ?: 1) - 1)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 480.dp).testTag("preset_grid"),
            ) {
                items((1..amp.presetCount).toList()) { preset ->
                    val pick = {
                        panel.recall(preset)
                        onClose()
                    }
                    val tag = Modifier.heightIn(min = 48.dp).testTag("preset:$preset")
                    val label: @Composable () -> Unit = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$preset", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            amp.slotOf(preset)?.let { slot ->
                                Text(
                                    "${slot.bank}-${slot.slot}",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                )
                            }
                        }
                    }
                    val padding = PaddingValues(0.dp)
                    if (preset == current) {
                        FilledTonalButton(onClick = pick, modifier = tag, contentPadding = padding) { label() }
                    } else {
                        OutlinedButton(onClick = pick, modifier = tag, contentPadding = padding) { label() }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onClose) { Text(stringResource(R.string.cancel)) } },
        modifier = Modifier.padding(8.dp),
    )
}

private const val PER_ROW = 8
