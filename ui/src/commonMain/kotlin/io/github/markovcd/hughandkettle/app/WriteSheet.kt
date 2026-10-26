package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.markovcd.hughandkettle.amp.PresetWriting
import io.github.markovcd.hughandkettle.amp.PresetWriting.Stage
import io.github.markovcd.hughandkettle.amp.PresetWriting.Why
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.close
import io.github.markovcd.hughandkettle.ui.done
import io.github.markovcd.hughandkettle.ui.write_cancelled
import io.github.markovcd.hughandkettle.ui.write_check
import io.github.markovcd.hughandkettle.ui.write_difference
import io.github.markovcd.hughandkettle.ui.write_differs
import io.github.markovcd.hughandkettle.ui.write_differs_head
import io.github.markovcd.hughandkettle.ui.write_hands_off
import io.github.markovcd.hughandkettle.ui.write_into
import io.github.markovcd.hughandkettle.ui.write_lost
import io.github.markovcd.hughandkettle.ui.write_no_backup
import io.github.markovcd.hughandkettle.ui.write_no_store
import io.github.markovcd.hughandkettle.ui.write_none
import io.github.markovcd.hughandkettle.ui.write_retry
import io.github.markovcd.hughandkettle.ui.write_store
import io.github.markovcd.hughandkettle.ui.write_stored
import io.github.markovcd.hughandkettle.ui.write_stored_closing
import io.github.markovcd.hughandkettle.ui.write_unchanged
import io.github.markovcd.hughandkettle.ui.write_unsure
import io.github.markovcd.hughandkettle.ui.write_unsure_why
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * A write of a tone into a preset from start to end, over the Tones tab. While the backup is taken it shows only
 * where the tone goes, since STORE pressed then would store into the preset before it is backed up; then it asks
 * for STORE on the amp, and ends on what came of it. A clean store closes by itself. Tests find the sheet by
 * `write_sheet`, the backup by `write_backing_up`, the ask by `write_waiting`, the end by `write_result:<result>`
 * (`stored`, `stored_with_differences`, `not_written` or `unsure`), the differences by `write_diffs` and the
 * buttons by `write_cancel`, `write_done`, `write_close`, `write_retry` and `write_check`.
 */
@Composable
internal fun WriteSheet(
    shelf: ToneShelf,
    writing: PresetWriting,
) {
    val where = presetWhere(writing.amp, writing.preset)
    val stage = writing.stage
    Dialog(
        onDismissRequest = shelf::closeWrite,
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.testTag("write_sheet"),
        ) {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (stage) {
                    Stage.BackingUp, Stage.Waiting, Stage.Storing -> {
                        Asking(where, asking = stage != Stage.BackingUp)
                        Buttons {
                            OutlinedButton(
                                onClick = shelf::cancelWrite,
                                enabled = stage != Stage.Storing,
                                modifier = Modifier.weight(1f).testTag("write_cancel"),
                            ) { Text(stringResource(Res.string.cancel)) }
                        }
                    }

                    is Stage.Stored -> {
                        Stored(stage, where, writing.tone.name)
                        Buttons {
                            Button(onClick = shelf::closeWrite, modifier = Modifier.weight(1f).testTag("write_done")) {
                                Text(stringResource(Res.string.done))
                            }
                        }
                        if (stage.differences.isEmpty()) {
                            LaunchedEffect(writing) {
                                delay(CLOSES_AFTER)
                                shelf.closeWrite()
                            }
                        }
                    }

                    is Stage.NotWritten -> {
                        Result(
                            stringResource(
                                when (stage.why) {
                                    Why.Cancelled -> Res.string.write_cancelled
                                    Why.LinkLost -> Res.string.write_lost
                                    else -> Res.string.write_none
                                },
                            ),
                            stringResource(
                                when (stage.why) {
                                    Why.NoStore -> Res.string.write_no_store
                                    Why.NoBackup -> Res.string.write_no_backup
                                    else -> Res.string.write_unchanged
                                },
                                where,
                            ),
                            "not_written",
                        )
                        Buttons {
                            OutlinedButton(
                                onClick = shelf::closeWrite,
                                modifier = Modifier.weight(1f).testTag("write_close"),
                            ) {
                                Text(stringResource(Res.string.close))
                            }
                            Button(
                                onClick = { shelf.write(writing.tone, writing.preset) },
                                enabled = shelf.canWrite(writing.tone),
                                modifier = Modifier.weight(1f).testTag("write_retry"),
                            ) { Text(stringResource(Res.string.write_retry)) }
                        }
                    }

                    Stage.Unsure -> {
                        Result(
                            stringResource(Res.string.write_unsure, where),
                            stringResource(Res.string.write_unsure_why),
                            "unsure",
                        )
                        Buttons {
                            OutlinedButton(
                                onClick = shelf::closeWrite,
                                modifier = Modifier.weight(1f).testTag("write_close"),
                            ) {
                                Text(stringResource(Res.string.close))
                            }
                            Button(
                                onClick = shelf::checkWrite,
                                enabled = shelf.canWrite(writing.tone),
                                modifier = Modifier.weight(1f).testTag("write_check"),
                            ) { Text(stringResource(Res.string.write_check, where)) }
                        }
                    }
                }
            }
        }
    }
}

/** Where the tone goes, and once the backup is kept the ask for STORE, large enough to read from the amp. */
@Composable
private fun Asking(
    where: String,
    asking: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.testTag(if (asking) "write_waiting" else "write_backing_up"),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            if (asking) stringResource(Res.string.write_store) else "",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
        )
        Text(
            stringResource(Res.string.write_into),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Text(where, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            stringResource(Res.string.write_hands_off),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

/** The store, and the values the amp kept otherwise than the tone. */
@Composable
private fun Stored(
    stored: Stage.Stored,
    where: String,
    name: String,
) {
    if (stored.differences.isEmpty()) {
        Result(
            stringResource(Res.string.write_stored, where, name),
            stringResource(Res.string.write_stored_closing),
            "stored",
        )
        return
    }
    val colors = MaterialTheme.colorScheme
    Result(stringResource(Res.string.write_differs, where, name), null, "stored_with_differences")
    Column(Modifier.testTag("write_diffs"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(Res.string.write_differs_head),
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        stored.differences.forEach { difference ->
            Row(Modifier.fillMaxWidth()) {
                Text(difference.control.name, Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text(stringResource(Res.string.write_difference, difference.stored, difference.tone))
            }
        }
    }
}

/** What came of the write: a [title] and what it means, tagged `write_result:<result>`. */
@Composable
private fun Result(
    title: String,
    body: String?,
    result: String,
) {
    Column(Modifier.testTag("write_result:$result"), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        body?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** A row of buttons sharing the width. */
@Composable
private fun Buttons(content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), content = content)
}

private val CLOSES_AFTER = 3.seconds
