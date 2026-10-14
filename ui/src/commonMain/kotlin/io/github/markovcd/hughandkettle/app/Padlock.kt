package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** The tempo lock's glyph on a 24-unit grid: a body under a shackle, swung open when [open], the body [solid] or outlined. */
@Composable
internal fun Padlock(
    open: Boolean,
    color: Color,
    size: Dp = 18.dp,
    solid: Boolean = false,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val stroke = Stroke(2 * u, cap = StrokeCap.Round)
        drawRoundRect(
            color = color,
            topLeft = Offset(5 * u, 11 * u),
            size = Size(14 * u, 10 * u),
            cornerRadius = CornerRadius(2 * u),
            style = if (solid) Fill else stroke,
        )
        // Closed, the shackle comes back down into the body; open, it stops short a third of the way round.
        val shackle =
            Path().apply {
                moveTo(8 * u, 11 * u)
                lineTo(8 * u, 8 * u)
                arcTo(Rect(8 * u, 4 * u, 16 * u, 12 * u), 180f, if (open) 150f else 180f, false)
                if (!open) lineTo(16 * u, 11 * u)
            }
        drawPath(shackle, color, style = stroke)
    }
}
