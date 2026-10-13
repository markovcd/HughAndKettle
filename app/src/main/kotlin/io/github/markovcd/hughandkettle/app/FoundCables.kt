package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R

/**
 * The USB MIDI cables plugged into the phone, one tappable row each, and nothing when there are none.
 * Tests find the rows by `cable:<id>`.
 */
@Composable
internal fun FoundCables(
    cables: List<Cable>,
    onPick: (Cable) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (cables.isEmpty()) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(R.string.cables_found),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        cables.forEach { cable ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onPick(cable) }
                        .padding(horizontal = 4.dp)
                        .testTag("cable:${cable.id}"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(cable.name, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
