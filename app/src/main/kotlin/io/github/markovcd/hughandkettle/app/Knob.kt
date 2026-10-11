package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The one custom-drawn control: an amber cap with a notch to its rim and a 270° arc for the value.
 *
 * Turning is a straight drag from anywhere in the knob: up or right turns up, down or left turns down.
 * Holding still for the long-press time before dragging gives fine mode. Nothing eases: the notch, the
 * arc and the value follow the finger, and [onValue] fires on every step.
 */
@Composable
fun Knob(
    value: Int,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    size: Dp = 76.dp,
) {
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current.density
    val current by rememberUpdatedState(value)
    val latestOnValue by rememberUpdatedState(onValue)

    fun move(to: Int) {
        val clamped = to.coerceIn(0, KnobDial.MAX)
        if (clamped == current) return
        if (KnobDial.reachesTick(current, clamped)) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        latestOnValue(clamped)
    }

    Canvas(
        modifier
            .size(size)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..KnobDial.MAX.toFloat(), KnobDial.MAX)
                setProgress { target ->
                    move(target.roundToInt())
                    true
                }
            }.focusable(enabled)
            .onKeyEvent { event ->
                if (!enabled || event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val step =
                    when (event.key) {
                        Key.DirectionUp, Key.DirectionRight -> 1
                        Key.DirectionDown, Key.DirectionLeft -> -1
                        Key.PageUp -> 8
                        Key.PageDown -> -8
                        else -> return@onKeyEvent false
                    }
                move(current + step)
                true
            }.pointerInput(enabled) {
                if (!enabled) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown()
                    val origin = down.position
                    val startValue = current
                    // Held still past the long-press time: fine mode for the rest of this drag.
                    val fine = awaitLongPressOrCancellation(down.id) != null
                    drag(down.id) { change ->
                        change.consume()
                        val delta = change.position - origin
                        move(KnobDial.valueAfterDrag(startValue, delta.x / density, delta.y / density, fine))
                    }
                }
            },
    ) {
        val stroke = 5.dp.toPx()
        val radius = this.size.minDimension / 2
        val arcRadius = radius - stroke / 2
        val arcBox = Size(arcRadius * 2, arcRadius * 2)
        val arcTopLeft = Offset(center.x - arcRadius, center.y - arcRadius)
        val cap = if (enabled) colors.primary else colors.onSurfaceVariant
        drawArc(
            color = colors.surfaceContainerHighest,
            startAngle = KnobDial.START_DEGREES,
            sweepAngle = KnobDial.SWEEP_DEGREES,
            useCenter = false,
            topLeft = arcTopLeft,
            size = arcBox,
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        if (value > 0) {
            drawArc(
                color = cap,
                startAngle = KnobDial.START_DEGREES,
                sweepAngle = KnobDial.angleDegrees(value) - KnobDial.START_DEGREES,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcBox,
                style = Stroke(stroke, cap = StrokeCap.Round),
            )
        }
        val capRadius = radius * 21f / 36f
        drawCircle(cap, radius = capRadius, center = center)
        val angle = Math.toRadians(KnobDial.angleDegrees(value).toDouble())
        val rim = Offset(center.x + capRadius * cos(angle).toFloat(), center.y + capRadius * sin(angle).toFloat())
        drawLine(colors.surface, start = center, end = rim, strokeWidth = stroke, cap = StrokeCap.Round)
    }
}
