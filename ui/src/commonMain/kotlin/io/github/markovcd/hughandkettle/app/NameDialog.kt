package io.github.markovcd.hughandkettle.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.ok
import org.jetbrains.compose.resources.stringResource

/**
 * Asks for a name. Tests find the field by `tone_name` and the confirm by `tone_name_done`; a blank
 * name is taken only when it does not [need a name][needsName].
 */
@Composable
fun NameDialog(
    title: String,
    label: String,
    initial: String,
    onDone: (String) -> Unit,
    onCancel: () -> Unit,
    needsName: Boolean = true,
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(label) },
                singleLine = true,
                modifier = Modifier.testTag("tone_name"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onDone(name) },
                enabled = name.isNotBlank() || !needsName,
                modifier = Modifier.testTag("tone_name_done"),
            ) {
                Text(stringResource(Res.string.ok))
            }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) } },
    )
}
