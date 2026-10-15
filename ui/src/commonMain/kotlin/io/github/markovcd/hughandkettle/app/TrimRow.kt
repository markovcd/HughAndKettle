package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.trim_down
import io.github.markovcd.hughandkettle.ui.trim_up
import io.github.markovcd.hughandkettle.ui.volume_trim
import org.jetbrains.compose.resources.stringResource

/**
 * The volume trim: the amount every recalled preset's and channel's volume is moved by, stepped down and up.
 * Tests find the amount by `trim`, the buttons by `trim:down` and `trim:up`.
 */
@Composable
internal fun TrimRow(
    trim: Int,
    enabled: Boolean,
    onTrim: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().height(56.dp).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.volume_trim),
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.weight(1f),
        )
        OutlinedButton(
            onClick = { onTrim((trim - STEP).coerceAtLeast(-127)) },
            enabled = enabled && trim > -127,
            modifier = Modifier.testTag("trim:down"),
        ) { Text(stringResource(Res.string.trim_down)) }
        Text(
            text = if (trim > 0) "+$trim" else "$trim",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 56.dp).testTag("trim"),
        )
        OutlinedButton(
            onClick = { onTrim((trim + STEP).coerceAtMost(127)) },
            enabled = enabled && trim < 127,
            modifier = Modifier.testTag("trim:up"),
        ) { Text(stringResource(Res.string.trim_up)) }
    }
}

/** How far one press moves the trim, in the knob's 0..127 steps. */
private const val STEP = 5
