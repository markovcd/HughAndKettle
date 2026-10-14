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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/** A tab's glyph, drawn in place: a knob for the amp, a wave for the effects, a bookmark for the tones. */
@Composable
fun TabGlyph(
    tab: PanelTab,
    selected: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val color = if (selected) colors.onSecondaryContainer else colors.onSurfaceVariant
    Canvas(Modifier.size(22.dp)) {
        val u = size.width / 24
        val stroke = Stroke(2 * u, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (tab) {
            PanelTab.Amp -> {
                drawCircle(color, 8 * u, Offset(12 * u, 12 * u), style = stroke)
                drawLine(color, Offset(12 * u, 12 * u), Offset(12 * u, 7 * u), 2 * u, StrokeCap.Round)
            }

            PanelTab.Fx -> {
                val wave =
                    Path().apply {
                        moveTo(2 * u, 12 * u)
                        lineTo(5 * u, 12 * u)
                        lineTo(7 * u, 6 * u)
                        lineTo(11 * u, 18 * u)
                        lineTo(14 * u, 9 * u)
                        lineTo(16 * u, 12 * u)
                        lineTo(22 * u, 12 * u)
                    }
                drawPath(wave, color, style = stroke)
            }

            PanelTab.Tones -> {
                val mark =
                    Path().apply {
                        moveTo(6 * u, 3 * u)
                        lineTo(18 * u, 3 * u)
                        lineTo(18 * u, 21 * u)
                        lineTo(12 * u, 17 * u)
                        lineTo(6 * u, 21 * u)
                        close()
                    }
                drawPath(mark, color, style = stroke)
            }
        }
    }
}
