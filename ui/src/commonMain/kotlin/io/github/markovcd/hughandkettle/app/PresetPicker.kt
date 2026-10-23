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
@OptIn(ExperimentalFoundationApi::class)
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
                val now = current?.let { (it - 1) / perRow } ?: 0
                LazyColumn(
                    state = rememberLazyListState(initialFirstVisibleItemIndex = now),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.heightIn(max = 480.dp).testTag("preset_picker_list"),
                ) {
                    items(rows) { row ->
                        Column(
                            Modifier.testTag("preset_bank:${row + 1}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            if (banked) {
                                Text(
                                    stringResource(
                                        if (row == now &&
                                            current != null
                                        ) {
                                            Res.string.preset_bank_now
                                        } else {
                                            Res.string.preset_bank
                                        },
                                        row + 1,
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color =
                                        if (row == now &&
                                            current != null
                                        ) {
                                            colors.primary
                                        } else {
                                            colors.onSurfaceVariant
                                        },
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..perRow).map { row * perRow + it }.forEach { preset ->
                                    PresetTile(
                                        preset,
                                        names.names[preset],
                                        lit = preset == current,
                                        onPick = {
                                            panel.recall(preset)
                                            onClose()
                                        },
                                        onHold = { renaming = preset },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
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

/** A preset's number large and its name small, or "No name" in italics; amber when [lit]. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PresetTile(
    preset: Int,
    name: String?,
    lit: Boolean,
    onPick: () -> Unit,
    onHold: () -> Unit,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Surface(
        shape = shape,
        color = if (lit) colors.primary else colors.surfaceContainerHigh,
        border = if (lit) null else BorderStroke(1.dp, colors.outlineVariant),
        modifier =
            modifier
                .clip(shape)
                .combinedClickable(role = Role.Button, onLongClick = onHold, onClick = onPick)
                .testTag("preset_pick:$preset"),
    ) {
        Column(
            Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "$preset",
                style = MaterialTheme.typography.headlineSmall,
                fontSize = 20.sp,
                color = if (lit) colors.onPrimary else colors.onSurface,
            )
            Text(
                name ?: stringResource(Res.string.preset_no_name),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 11.sp,
                fontStyle = if (name == null) FontStyle.Italic else FontStyle.Normal,
                color = if (lit) colors.onPrimary else colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
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

private const val PER_ROW = 4
