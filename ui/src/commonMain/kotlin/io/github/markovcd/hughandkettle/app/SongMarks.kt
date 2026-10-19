package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A warning triangle with a bang, on a 24-unit grid. */
@Composable
internal fun WarningMark(
    color: Color,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(size)) {
        val u = this.size.width / 24
        val stroke = Stroke(2.25f * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        val warning =
            Path().apply {
                moveTo(12 * u, 3 * u)
                lineTo(22 * u, 21 * u)
                lineTo(2 * u, 21 * u)
                close()
            }
        drawPath(warning, color, style = stroke)
        drawLine(color, Offset(12 * u, 10 * u), Offset(12 * u, 15 * u), 2.25f * u, StrokeCap.Round)
        drawLine(color, Offset(12 * u, 18 * u), Offset(12 * u, 18.5f * u), 2.25f * u, StrokeCap.Round)
    }
}

/** A tick, on a 24-unit grid. */
@Composable
internal fun CheckMark(
    color: Color,
    size: Dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val tick =
            Path().apply {
                moveTo(5 * u, 12.5f * u)
                lineTo(10 * u, 17.5f * u)
                lineTo(19 * u, 7 * u)
            }
        drawPath(tick, color, style = Stroke(2.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A chevron pointing down, on a 24-unit grid. */
@Composable
internal fun ChevronDown(
    color: Color,
    size: Dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val chevron =
            Path().apply {
                moveTo(6 * u, 9 * u)
                lineTo(12 * u, 15 * u)
                lineTo(18 * u, 9 * u)
            }
        drawPath(chevron, color, style = Stroke(2.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Three dots one over another, the mark of a menu. */
@Composable
internal fun MenuDots(color: Color) {
    Canvas(Modifier.size(4.dp, 18.dp)) {
        val radius = size.width / 2
        listOf(radius, size.height / 2, size.height - radius).forEach { drawCircle(color, radius, Offset(radius, it)) }
    }
}
