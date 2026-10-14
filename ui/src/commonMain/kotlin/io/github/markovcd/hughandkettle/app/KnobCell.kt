package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control

/**
 * A knob with its name above and its value below, in a 150 dp cell: in the amp's unit where the manual
 * gives a scale. Tests find the value by `value:<id>`.
 */
@Composable
internal fun KnobCell(
    control: Control,
    value: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = control.scale
    Column(
        modifier = modifier.height(150.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(control.name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 2)
        Knob(value = value, onValue = onValue, description = control.name, enabled = enabled)
        Text(
            text = if (scale == null) "$value" else "${scale.amount(value)} ${scale.unit}",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.testTag("value:${control.id}"),
        )
    }
}
