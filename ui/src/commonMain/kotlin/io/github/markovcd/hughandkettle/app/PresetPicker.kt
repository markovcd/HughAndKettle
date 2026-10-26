package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.close
import io.github.markovcd.hughandkettle.ui.preset_bank
import io.github.markovcd.hughandkettle.ui.preset_bank_now
import io.github.markovcd.hughandkettle.ui.preset_name_title
import io.github.markovcd.hughandkettle.ui.preset_no_name
import io.github.markovcd.hughandkettle.ui.preset_pick
import io.github.markovcd.hughandkettle.ui.preset_picker_banks
import io.github.markovcd.hughandkettle.ui.preset_picker_count
import io.github.markovcd.hughandkettle.ui.preset_picker_naming
import io.github.markovcd.hughandkettle.ui.tone_name
import org.jetbrains.compose.resources.stringResource

/**
 * Every preset of the panel's amp in rows of four, a row per bank where the amp has banks, opened on the
 * current preset's row, which is marked, and the current preset amber. A tap recalls a preset and closes the
 * picker; holding one asks for its name. Tests find the picker by `preset_picker`, its list by
 * `preset_picker_list`, a row by `preset_bank:<n>`, a preset by `preset_pick:<n>` and the close by
 * `preset_picker_close`.
 */
@Composable
internal fun PresetPicker(
    panel: PanelState,
    names: PresetNamesState,
    onClose: () -> Unit,
) {
    val amp = panel.amp
    val current = panel.state.preset
    val perRow = amp.presetsPerBank ?: PER_ROW
    val rows = amp.presetCount / perRow
    val banked = amp.presetsPerBank != null
    var renaming by remember { mutableStateOf<Int?>(null) }
    val colors = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onClose) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.surfaceContainer,
            modifier = Modifier.testTag("preset_picker"),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PickerTitle(
                    stringResource(Res.string.preset_pick),
                    if (banked) {
                        stringResource(Res.string.preset_picker_banks, rows, perRow)
                    } else {
                        stringResource(Res.string.preset_picker_count, amp.presetCount)
                    },
                    onClose,
                    "preset_picker_close",
                )
                Text(
                    stringResource(Res.string.preset_picker_naming),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
                names.failure?.let { Text(it, color = colors.error) }
                PresetGrid(
                    amp,
                    current,
                    names.names,
                    lit = current,
                    listTag = "preset_picker_list",
                    tileTag = "preset_pick",
                    onPick = { preset ->
                        panel.recall(preset)
                        onClose()
                    },
                    onHold = { renaming = it },
                )
            }
        }
    }
    renaming?.let { preset ->
        NameDialog(
            title = stringResource(Res.string.preset_name_title, preset),
            label = stringResource(Res.string.tone_name),
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

/** A picker's [title] over its [subtitle], with a close button tagged [closeTag]. */
@Composable
internal fun PickerTitle(
    title: String,
    subtitle: String,
    onClose: () -> Unit,
    closeTag: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onClose, modifier = Modifier.testTag(closeTag)) { Text(stringResource(Res.string.close)) }
    }
    Spacer(Modifier.height(4.dp))
}

internal const val PER_ROW = 4
