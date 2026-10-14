package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control

/** A switch beside the amp's name, as mute: a round toggle with a struck-out speaker, ringed in amber while on. */
@Composable
fun HeaderSwitch(
    control: Control,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val on = control.isOn(panel.state.values[control.id] ?: 0)
    val colors = MaterialTheme.colorScheme
    OutlinedIconToggleButton(
        checked = on,
        onCheckedChange = { panel.set(control, if (it) 127 else 0) },
        enabled = enabled,
        shape = CircleShape,
        border = BorderStroke(1.dp, if (on) colors.primary else colors.outlineVariant),
        modifier =
            modifier
                .size(
                    44.dp,
                ).semantics { contentDescription = control.name }
                .testTag("control:${control.id}"),
    ) {
        val color = if (on) colors.primary else colors.onSurfaceVariant
        Canvas(Modifier.size(20.dp)) {
            val u = size.width / 24
            val stroke = Stroke(2 * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val speaker =
                Path().apply {
                    moveTo(11 * u, 5 * u)
                    lineTo(6 * u, 9 * u)
                    lineTo(3 * u, 9 * u)
                    lineTo(3 * u, 15 * u)
                    lineTo(6 * u, 15 * u)
                    lineTo(11 * u, 19 * u)
                    close()
                }
            drawPath(speaker, color, style = stroke)
            drawLine(color, Offset(16 * u, 9.5f * u), Offset(21 * u, 14.5f * u), 2 * u, StrokeCap.Round)
            drawLine(color, Offset(21 * u, 9.5f * u), Offset(16 * u, 14.5f * u), 2 * u, StrokeCap.Round)
        }
    }
}
