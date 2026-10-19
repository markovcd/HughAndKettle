package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.song_name
import io.github.markovcd.hughandkettle.ui.song_name_taken
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for a song's name, refusing a blank one and one another song of the amp already has, with the reason
 * under the field. Tests find the field by `song_name` and the buttons by `<tag>_cancel` and [doneTag].
 */
@Composable
internal fun SongNameDialog(
    title: String,
    done: String,
    tag: String,
    doneTag: String,
    initial: String,
    taken: (String) -> Boolean,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var name by remember { mutableStateOf(initial) }
    val trimmed = name.trim()
    val clash = trimmed.isNotEmpty() && !trimmed.equals(initial, ignoreCase = true) && taken(trimmed)
    val colors = MaterialTheme.colorScheme
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(Res.string.song_name).uppercase()) },
                    singleLine = true,
                    isError = clash,
                    modifier = Modifier.testTag("song_name"),
                )
                if (clash) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        WarningMark(colors.error, 18.dp)
                        Text(
                            stringResource(Res.string.song_name_taken, trimmed),
                            fontSize = 14.sp,
                            color = colors.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onDone(trimmed) },
                enabled = trimmed.isNotEmpty() && !clash,
                modifier = Modifier.testTag(doneTag),
            ) { Text(done) }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("${tag}_cancel")) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
