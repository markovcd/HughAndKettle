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
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A speaker struck out with a cross: the mute. */
@Composable
internal fun MuteMark(
    color: Color,
    size: Dp = 20.dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
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

/** A push pin, filled when [filled]: a tone pinned to the header. */
@Composable
internal fun PinMark(
    color: Color,
    filled: Boolean,
    size: Dp = 20.dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val head =
            Path().apply {
                moveTo(8 * u, 3 * u)
                lineTo(16 * u, 3 * u)
                lineTo(15 * u, 10 * u)
                lineTo(18 * u, 14 * u)
                lineTo(6 * u, 14 * u)
                lineTo(9 * u, 10 * u)
                close()
            }
        drawPath(head, color, style = if (filled) Fill else Stroke(2 * u, join = StrokeJoin.Round))
        drawLine(color, Offset(12 * u, 14 * u), Offset(12 * u, 21 * u), 2 * u, StrokeCap.Round)
    }
}

/** A chevron pointing right: a row that opens more. */
@Composable
internal fun ChevronMark(
    color: Color,
    size: Dp = 18.dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val chevron =
            Path().apply {
                moveTo(9 * u, 6 * u)
                lineTo(15 * u, 12 * u)
                lineTo(9 * u, 18 * u)
            }
        drawPath(chevron, color, style = Stroke(2 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** A tick: the one picked. */
@Composable
internal fun TickMark(
    color: Color,
    size: Dp = 18.dp,
) {
    Canvas(Modifier.size(size)) {
        val u = this.size.width / 24
        val tick =
            Path().apply {
                moveTo(5 * u, 12 * u)
                lineTo(10 * u, 17 * u)
                lineTo(19 * u, 7 * u)
            }
        drawPath(tick, color, style = Stroke(3 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

/** Three dots stacked: the menu. */
@Composable
internal fun DotsMark(color: Color) {
    Canvas(Modifier.size(4.dp, 18.dp)) {
        val radius = size.width / 2
        listOf(radius, size.height / 2, size.height - radius).forEach {
            drawCircle(color, radius, Offset(radius, it))
        }
    }
}
