package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.amp_question
import io.github.markovcd.hughandkettle.ui.ok
import org.jetbrains.compose.resources.stringResource

/**
 * Asks which amp is behind a box linked to for the first time, the amp on the panel marked. Tests find it
 * by `amp_question` and each amp by `amp_answer:<id>`.
 */
@Composable
internal fun AmpQuestion(panel: PanelState) {
    AlertDialog(
        onDismissRequest = panel::settle,
        title = { Text(stringResource(Res.string.amp_question)) },
        text = {
            Column(Modifier.testTag("amp_question")) {
                AmpModels.all.forEach { amp ->
                    DropdownMenuItem(
                        text = { Text(amp.name) },
                        leadingIcon = { RadioButton(selected = amp == panel.amp, onClick = null) },
                        onClick = { panel.choose(amp) },
                        modifier = Modifier.heightIn(min = 48.dp).testTag("amp_answer:${amp.id}"),
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = panel::settle) { Text(stringResource(Res.string.ok)) } },
    )
}
