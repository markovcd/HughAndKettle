package io.github.markovcd.hughandkettle.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Tone

/**
 * The tones on the phone: save the panel as one, read the amp's presets into them, tap one to recall
 * it, and a menu per tone to move, rename, export or delete it. Tests find the parts by `tone_save`,
 * `tone_import`, `tone_read`, `tone_read_confirm`, `tone_read_stop`, `tone:<name>`, `tone_menu:<name>`
 * and the menu's `tone_delete` and kin, and the dialog's field by `tone_name`.
 */
@Composable
internal fun ToneShelfCard(
    shelf: ToneShelf,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val resolver = LocalContext.current.contentResolver
    val colors = MaterialTheme.colorScheme
    var naming by remember { mutableStateOf<ToneNaming?>(null) }
    var deleting by remember { mutableStateOf<Tone?>(null) }
    var exporting by remember { mutableStateOf<Tone?>(null) }
    var confirmingRead by remember { mutableStateOf(false) }
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TONE_FILE_TYPE)) { uri ->
            val tone = exporting
            exporting = null
            if (uri != null && tone != null) shelf.export(tone) { resolver.openOutputStream(uri) }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) shelf.import { resolver.openInputStream(uri) }
        }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.tones),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { import.launch(TONE_FILE_TYPES) }, modifier = Modifier.testTag("tone_import")) {
                    Text(stringResource(R.string.tone_import))
                }
                Button(
                    onClick = { naming = ToneNaming.New },
                    enabled = shelf.canSave,
                    modifier = Modifier.testTag("tone_save"),
                ) { Text(stringResource(R.string.tone_save)) }
            }
            val reading = shelf.reading
            if (reading == null) {
                TextButton(
                    onClick = { confirmingRead = true },
                    enabled = enabled && shelf.canRead,
                    modifier = Modifier.testTag("tone_read"),
                ) { Text(stringResource(R.string.tone_read)) }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(
                            R.string.tone_reading,
                            reading.preset ?: reading.amp.presetCount,
                            reading.amp.presetCount,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = shelf::stopReading, modifier = Modifier.testTag("tone_read_stop")) {
                        Text(stringResource(R.string.tone_read_stop))
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
                    stringResource(R.string.tones_none),
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
                    onExport = {
                        exporting = tone
                        export.launch("${tone.name}.json")
                    },
                    onDelete = { deleting = tone },
                )
            }
        }
    }
    naming?.let { current ->
        NameDialog(
            title = stringResource(if (current is ToneNaming.Existing) R.string.tone_rename else R.string.tone_save),
            label = stringResource(R.string.tone_name),
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
            text = { Text(stringResource(R.string.tone_read_explain)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingRead = false
                    shelf.readPresets()
                }, modifier = Modifier.testTag("tone_read_confirm")) { Text(stringResource(R.string.tone_read)) }
            },
            dismissButton = {
                TextButton(
                    onClick = { confirmingRead = false },
                ) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
    deleting?.let { tone ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            text = { Text(stringResource(R.string.tone_delete_confirm, tone.name)) },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    shelf.delete(tone)
                }, modifier = Modifier.testTag("tone_delete_confirm")) { Text(stringResource(R.string.tone_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
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
) {
    var menu by remember { mutableStateOf(false) }
    val recallable = enabled && shelf.fits(tone)
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(
            Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .clickable(enabled = recallable) { shelf.recall(tone) }
                .padding(vertical = 8.dp)
                .testTag("tone:${tone.name}"),
        ) {
            Text(
                tone.name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (recallable) colors.onSurface else colors.onSurfaceVariant,
            )
            if (!shelf.fits(tone)) {
                Text(
                    tone.amp.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Box {
            TextButton(onClick = { menu = true }, modifier = Modifier.testTag("tone_menu:${tone.name}")) { Text("⋮") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                listOf(
                    Triple("tone_move_up", R.string.tone_move_up) { shelf.move(tone, -1) },
                    Triple("tone_move_down", R.string.tone_move_down) { shelf.move(tone, 1) },
                    Triple("tone_rename", R.string.tone_rename, onRename),
                    Triple("tone_export", R.string.tone_export, onExport),
                    Triple("tone_delete", R.string.tone_delete, onDelete),
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
            stringResource(R.string.tone_failed, notice.reason)
        }

        is ToneNotice.Read -> {
            if (notice.missed.isEmpty()) {
                stringResource(R.string.tone_read_done, notice.count)
            } else {
                stringResource(R.string.tone_read_missed, notice.count, notice.missed.joinToString(", "))
            }
        }

        is ToneNotice.Imported -> {
            if (notice.skipped.isEmpty()) {
                stringResource(R.string.tone_imported, notice.name)
            } else {
                stringResource(R.string.tone_imported_skipped, notice.name, notice.skipped.joinToString("; "))
            }
        }

        is ToneNotice.ImportedPresets -> {
            if (notice.skipped.isEmpty()) {
                stringResource(R.string.tone_imported_presets, notice.count)
            } else {
                stringResource(R.string.tone_imported_presets_skipped, notice.count, notice.skipped.joinToString("; "))
            }
        }
    }

private const val TONE_FILE_TYPE = "application/json"

// File managers label a .json file in more than one way.
private val TONE_FILE_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain")
