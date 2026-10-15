package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** A round 44 dp button stepping a value down, or up when [up], drawn as a minus or a plus in amber. */
@Composable
internal fun RoundButton(
    up: Boolean,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier
            .size(44.dp)
            .alpha(if (enabled) 1f else DISABLED)
            .clip(CircleShape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val u = size.width / 24
            val ink = colors.primary
            drawLine(ink, Offset(5 * u, 12 * u), Offset(19 * u, 12 * u), 2.5f * u, StrokeCap.Round)
            if (up) drawLine(ink, Offset(12 * u, 5 * u), Offset(12 * u, 19 * u), 2.5f * u, StrokeCap.Round)
        }
    }
}

private const val DISABLED = 0.38f
