package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.wmi.LinkState

/** The link's state at a glance: a warning, a filled dot when linked, a spinner while linking, else a ring. */
@Composable
fun LinkGlyph(panel: PanelState) {
    val colors = MaterialTheme.colorScheme
    when {
        panel.warning -> {
            WarningTriangle(colors.error)
        }

        panel.linked -> {
            Box(Modifier.size(14.dp).background(colors.primary, CircleShape))
        }

        panel.link is LinkState.Connecting || panel.cable is CableState.Opening -> {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.4.dp,
            )
        }

        else -> {
            Box(Modifier.size(14.dp).border(2.dp, colors.outline, CircleShape))
        }
    }
}

@Composable
private fun WarningTriangle(color: Color) {
    Canvas(Modifier.size(18.dp, 16.dp)) {
        val w = size.width
        val h = size.height
        val stroke = 2.dp.toPx()
        val outline =
            Path().apply {
                moveTo(w / 2, stroke / 2)
                lineTo(w - stroke / 2, h - stroke / 2)
                lineTo(stroke / 2, h - stroke / 2)
                close()
            }
        drawPath(outline, color, style = Stroke(stroke, join = StrokeJoin.Round))
        drawLine(color, Offset(w / 2, h * 0.4f), Offset(w / 2, h * 0.62f), strokeWidth = stroke)
        drawCircle(color, radius = stroke * 0.55f, center = Offset(w / 2, h * 0.77f))
    }
}
