package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control

/** A switch with nothing behind it, as the noise gate or the FX loop, on a card of its own. */
@Composable
internal fun SwitchCard(
    control: Control,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
    ) {
        ControlRow(listOf(control), panel, enabled)
    }
}
