package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.LfoShape

/** An LFO's shape drawn as two cycles of its wave, on a 28 by 16 grid. */
@Composable
internal fun LfoGlyph(
    shape: LfoShape,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier.size(26.dp, 15.dp)) {
        val u = size.width / 28
        val path =
            Path().apply {
                when (shape) {
                    LfoShape.Sine -> {
                        moveTo(2 * u, 8 * u)
                        cubicTo(6 * u, 0f, 10 * u, 0f, 14 * u, 8 * u)
                        cubicTo(18 * u, 16 * u, 22 * u, 16 * u, 26 * u, 8 * u)
                    }

                    else -> {
                        points(shape).forEachIndexed { index, (x, y) ->
                            if (index == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u)
                        }
                    }
                }
            }
        drawPath(path, color, style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun points(shape: LfoShape): List<Pair<Float, Float>> =
    when (shape) {
        LfoShape.Sine -> {
            emptyList()
        }

        LfoShape.Triangle -> {
            listOf(2f to 12f, 8f to 4f, 14f to 12f, 20f to 4f, 26f to 12f)
        }

        LfoShape.Square -> {
            listOf(
                2f to 12f,
                2f to 4f,
                9f to 4f,
                9f to 12f,
                16f to 12f,
                16f to 4f,
                23f to 4f,
                23f to 12f,
                26f to 12f,
            )
        }

        LfoShape.RampUp -> {
            listOf(2f to 12f, 12f to 4f, 12f to 12f, 22f to 4f, 22f to 12f)
        }

        LfoShape.RampDown -> {
            listOf(2f to 4f, 12f to 12f, 12f to 4f, 22f to 12f, 22f to 4f)
        }

        LfoShape.Random -> {
            listOf(
                2f to 10f,
                6f to 10f,
                6f to 4f,
                10f to 4f,
                10f to 12f,
                14f to 12f,
                14f to 6f,
                18f to 6f,
                18f to 10f,
                22f to 10f,
                22f to 3f,
                26f to 3f,
            )
        }
    }
