package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** A dashed outline of [color] with rounded corners. */
internal fun Modifier.dashed(
    color: Color,
    radius: Dp,
): Modifier =
    drawBehind {
        drawRoundRect(
            color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }
