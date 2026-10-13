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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/**
 * Every preset of the panel's amp, eight to a row, the current one filled, each with the name given
 * to it. A tap recalls it and closes the grid; while naming, a tap asks for its name instead. Tests
 * find the grid by `preset_grid`, each preset by `preset:<n>` and the naming switch by `preset_naming`.
 */
@Composable
internal fun PresetGrid(
    panel: PanelState,
    names: PresetNamesState,
    onClose: () -> Unit,
) {
    val amp = panel.amp
    val current = panel.state.preset
    var naming by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Int?>(null) }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(stringResource(if (naming) R.string.preset_naming_title else R.string.preset_pick)) },
        text = {
            names.failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            LazyVerticalGrid(
                columns = GridCells.Fixed(PER_ROW),
                state = rememberLazyGridState(initialFirstVisibleItemIndex = ((current ?: 1) - 1)),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.heightIn(max = 480.dp).testTag("preset_grid"),
            ) {
                items((1..amp.presetCount).toList()) { preset ->
                    val pick = {
                        if (naming) {
                            renaming = preset
                        } else {
                            panel.recall(preset)
                            onClose()
                        }
                    }
                    val tag = Modifier.heightIn(min = 48.dp).testTag("preset:$preset")
                    val label: @Composable () -> Unit = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$preset", style = MaterialTheme.typography.labelLarge, maxLines = 1)
                            val under = names.names[preset] ?: amp.slotOf(preset)?.let { "${it.bank}-${it.slot}" }
                            under?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
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
        dismissButton = {
            TextButton(onClick = { naming = !naming }, modifier = Modifier.testTag("preset_naming")) {
                Text(stringResource(if (naming) R.string.preset_naming_done else R.string.preset_naming))
            }
        },
        modifier = Modifier.padding(8.dp),
    )
    renaming?.let { preset ->
        NameDialog(
            title = stringResource(R.string.preset_name_title, preset),
            label = stringResource(R.string.tone_name),
            initial = names.names[preset] ?: "",
            onDone = { name ->
                renaming = null
                names.name(preset, name)
            },
            onCancel = { renaming = null },
            needsName = false,
        )
    }
}

private const val PER_ROW = 8
