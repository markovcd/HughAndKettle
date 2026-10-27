package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A cross: closing, or taking something away, named [description]. */
@Composable
internal fun CrossMark(description: String) {
    val ink = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(22.dp).semantics { contentDescription = description }) {
        val u = size.width / 24
        drawLine(ink, Offset(6 * u, 6 * u), Offset(18 * u, 18 * u), 2 * u, StrokeCap.Round)
        drawLine(ink, Offset(18 * u, 6 * u), Offset(6 * u, 18 * u), 2 * u, StrokeCap.Round)
    }
}

/** A five-pointed star, filled in amber when [filled]: the loudness match's reference, named [description]. */
@Composable
internal fun StarMark(
    filled: Boolean,
    description: String,
) {
    val colors = MaterialTheme.colorScheme
    Canvas(Modifier.size(22.dp).semantics { contentDescription = description }) {
        val u = size.width / 24
        val star =
            Path().apply {
                for (i in 0 until POINTS * 2) {
                    val radius = if (i % 2 == 0) 10 * u else 4.2f * u
                    val angle = -PI / 2 + i * PI / POINTS
                    val x = 12 * u + radius * cos(angle).toFloat()
                    val y = 12.5f * u + radius * sin(angle).toFloat()
                    if (i == 0) moveTo(x, y) else lineTo(x, y)
                }
                close()
            }
        if (filled) {
            drawPath(star, colors.primary, style = Fill)
        } else {
            drawPath(star, colors.onSurfaceVariant, style = Stroke(1.8f * u, join = StrokeJoin.Round))
        }
    }
}

private const val POINTS = 5
