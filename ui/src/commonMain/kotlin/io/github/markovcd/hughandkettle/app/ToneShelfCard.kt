package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.IconToggleButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.tone_backed_up
import io.github.markovcd.hughandkettle.ui.tone_delete
import io.github.markovcd.hughandkettle.ui.tone_delete_confirm
import io.github.markovcd.hughandkettle.ui.tone_export
import io.github.markovcd.hughandkettle.ui.tone_failed
import io.github.markovcd.hughandkettle.ui.tone_import
import io.github.markovcd.hughandkettle.ui.tone_imported
import io.github.markovcd.hughandkettle.ui.tone_imported_presets
import io.github.markovcd.hughandkettle.ui.tone_imported_presets_skipped
import io.github.markovcd.hughandkettle.ui.tone_imported_skipped
import io.github.markovcd.hughandkettle.ui.tone_loaded
import io.github.markovcd.hughandkettle.ui.tone_move_down
import io.github.markovcd.hughandkettle.ui.tone_move_up
import io.github.markovcd.hughandkettle.ui.tone_name
import io.github.markovcd.hughandkettle.ui.tone_pin
import io.github.markovcd.hughandkettle.ui.tone_read
import io.github.markovcd.hughandkettle.ui.tone_read_done
import io.github.markovcd.hughandkettle.ui.tone_read_explain
import io.github.markovcd.hughandkettle.ui.tone_read_missed
import io.github.markovcd.hughandkettle.ui.tone_read_stop
import io.github.markovcd.hughandkettle.ui.tone_reading
import io.github.markovcd.hughandkettle.ui.tone_rename
import io.github.markovcd.hughandkettle.ui.tone_save
import io.github.markovcd.hughandkettle.ui.tone_write
import io.github.markovcd.hughandkettle.ui.tone_write_what
import io.github.markovcd.hughandkettle.ui.tones
import io.github.markovcd.hughandkettle.ui.tones_none
import io.github.markovcd.hughandkettle.ui.tones_pinned
import org.jetbrains.compose.resources.stringResource

/**
 * The tones on the phone: save the panel as one, read the amp's presets into them, tap one to recall
 * it, pin it to the header, and a menu per tone, behind its dots or a right-click, to write it into one of the
 * amp's presets, move, rename, export or delete it. The tone last loaded is outlined amber and says so. Tests find
 * the parts by `tone_save`, `tone_import`, `tone_read`, `tone_read_confirm`, `tone_read_stop`, `tones_pinned`,
 * `tone:<name>`, `tone_pin:<name>`, `tone_loaded:<name>`, `tone_menu:<name>` and the menu's `tone_write`,
 * `tone_delete` and kin, and the dialog's field by `tone_name`.
 */
@Composable
fun ToneShelfCard(
    shelf: ToneShelf,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val files = rememberToneFiles()
    val colors = MaterialTheme.colorScheme
    var naming by remember { mutableStateOf<ToneNaming?>(null) }
    var deleting by remember { mutableStateOf<Tone?>(null) }
    var confirmingRead by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<Tone?>(null) }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.tones),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { files.open { shelf.import(it) } }, modifier = Modifier.testTag("tone_import")) {
                    Text(stringResource(Res.string.tone_import))
                }
                Button(
                    onClick = { naming = ToneNaming.New },
                    enabled = shelf.canSave,
                    modifier = Modifier.testTag("tone_save"),
                ) { Text(stringResource(Res.string.tone_save)) }
            }
            val fitting = shelf.tones.count(shelf::fits)
            if (fitting > 0) {
                Text(
                    stringResource(Res.string.tones_pinned, shelf.pinned.size, fitting),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.testTag("tones_pinned"),
                )
            }
            val reading = shelf.reading
            if (reading == null) {
                TextButton(
                    onClick = { confirmingRead = true },
                    enabled = enabled && shelf.canRead,
                    modifier = Modifier.testTag("tone_read"),
                ) { Text(stringResource(Res.string.tone_read)) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            Res.string.tone_reading,
                            reading.preset ?: reading.amp.presetCount,
                            reading.amp.presetCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = shelf::stopReading, modifier = Modifier.testTag("tone_read_stop")) {
                        Text(stringResource(Res.string.tone_read_stop))
                    }
                }
            }
            shelf.notice?.let { notice ->
                Text(
                    text = words(notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (notice is ToneNotice.Failed) colors.error else colors.onSurface,
                    modifier = Modifier.clickable(onClick = shelf::dismiss).testTag("tone_notice"),
                )
            }
            if (shelf.tones.isEmpty()) {
                Text(
                    stringResource(Res.string.tones_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            shelf.tones.forEach { tone ->
                ToneRow(
                    tone = tone,
                    shelf = shelf,
                    enabled = enabled,
                    onRename = { naming = ToneNaming.Existing(tone) },
                    onExport = { files.create("${tone.name}.json") { shelf.export(tone, it) } },
                    onDelete = { deleting = tone },
                    onWrite = { picking = tone },
                )
            }
        }
    }
    naming?.let { current ->
        NameDialog(
            title =
                stringResource(
                    if (current is ToneNaming.Existing) Res.string.tone_rename else Res.string.tone_save,
                ),
            label = stringResource(Res.string.tone_name),
            initial = (current as? ToneNaming.Existing)?.tone?.name ?: "",
            onDone = { name ->
                naming = null
                if (current is ToneNaming.Existing) shelf.rename(current.tone, name) else shelf.save(name)
            },
            onCancel = { naming = null },
        )
    }
    if (confirmingRead) {
        AlertDialog(
            onDismissRequest = { confirmingRead = false },
            text = { Text(stringResource(Res.string.tone_read_explain)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingRead = false
                    shelf.readPresets()
                }, modifier = Modifier.testTag("tone_read_confirm")) { Text(stringResource(Res.string.tone_read)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingRead = false },
                ) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
    picking?.let { tone -> WritePicker(shelf, tone, onClose = { picking = null }) }
    shelf.writing?.let { WriteSheet(shelf, it) }
    deleting?.let { tone ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(Res.string.tone_delete_confirm, tone.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    shelf.delete(tone)
                }, modifier = Modifier.testTag("tone_delete_confirm")) { Text(stringResource(Res.string.tone_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(Res.string.cancel)) } },
        )
    }
}

@Composable
private fun ToneRow(
    tone: Tone,
    shelf: ToneShelf,
    enabled: Boolean,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
    onWrite: () -> Unit,
) {
    var menu by remember { mutableStateOf(false) }
    val recallable = enabled && shelf.fits(tone)
    val colors = MaterialTheme.colorScheme
    val loaded = tone == shelf.loaded
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .clip(shape)
            .border(1.dp, if (loaded) colors.primary else Color.Transparent, shape)
            .onSecondaryClick { menu = true }
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clickable(enabled = recallable) { shelf.recall(tone) }
                .padding(vertical = 8.dp)
                .testTag("tone:${tone.name}"),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    tone.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (recallable) colors.onSurface else colors.onSurfaceVariant,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (loaded) {
                    Surface(
                        shape = CircleShape,
                        color = colors.primary,
                        modifier = Modifier.testTag("tone_loaded:${tone.name}"),
                    ) {
                        Text(
                            stringResource(Res.string.tone_loaded),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.onPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            if (!shelf.fits(tone)) {
                Text(
                    tone.amp.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        val pinning = stringResource(Res.string.tone_pin, tone.name)
        IconToggleButton(
            checked = tone.pinned,
            onCheckedChange = { shelf.pin(tone, it) },
            modifier = Modifier.testTag("tone_pin:${tone.name}").semantics { contentDescription = pinning },
        ) { PinMark(if (tone.pinned) colors.primary else colors.onSurfaceVariant, filled = tone.pinned) }
        Box {
            TextButton(onClick = { menu = true }, modifier = Modifier.testTag("tone_menu:${tone.name}")) { Text("⋮") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(stringResource(Res.string.tone_write))
                            Text(
                                stringResource(Res.string.tone_write_what),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
                            )
                        }
                    },
                    onClick = {
                        menu = false
                        onWrite()
                    },
                    enabled = shelf.canWrite(tone),
                    modifier = Modifier.testTag("tone_write"),
                )
                listOf(
                    Triple("tone_move_up", Res.string.tone_move_up) { shelf.move(tone, -1) },
                    Triple("tone_move_down", Res.string.tone_move_down) { shelf.move(tone, 1) },
                    Triple("tone_rename", Res.string.tone_rename, onRename),
                    Triple("tone_export", Res.string.tone_export, onExport),
                    Triple("tone_delete", Res.string.tone_delete, onDelete),
                ).forEach { (tag, label, action) ->
                    DropdownMenuItem(
                        text = { Text(stringResource(label)) },
                        onClick = {
                            menu = false
                            action()
                        },
                        modifier = Modifier.testTag(tag),
                    )
                }
            }
        }
    }
}

@Composable
private fun words(notice: ToneNotice): String =
    when (notice) {
        is ToneNotice.Failed -> {
            stringResource(Res.string.tone_failed, notice.reason)
        }

        is ToneNotice.BackedUp -> {
            stringResource(Res.string.tone_backed_up, notice.name)
        }

        is ToneNotice.Read -> {
            if (notice.missed.isEmpty()) {
                stringResource(Res.string.tone_read_done, notice.count)
            } else {
                stringResource(Res.string.tone_read_missed, notice.count, notice.missed.joinToString(", "))
            }
        }

        is ToneNotice.Imported -> {
            if (notice.skipped.isEmpty()) {
                stringResource(Res.string.tone_imported, notice.name)
            } else {
                stringResource(Res.string.tone_imported_skipped, notice.name, notice.skipped.joinToString("; "))
            }
        }

        is ToneNotice.ImportedPresets -> {
            if (notice.skipped.isEmpty()) {
                stringResource(Res.string.tone_imported_presets, notice.count)
            } else {
                stringResource(
                    Res.string.tone_imported_presets_skipped,
                    notice.count,
                    notice.skipped.joinToString("; "),
                )
            }
        }
    }
