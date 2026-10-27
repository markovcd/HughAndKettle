package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.Lfo
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.lfo_range
import io.github.markovcd.hughandkettle.ui.lfo_range_high
import io.github.markovcd.hughandkettle.ui.lfo_range_low
import io.github.markovcd.hughandkettle.ui.lfo_range_what
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * The knob an LFO sweeps, in place of its own: the swing between the two ends lit on the arc, a handle at
 * each end, and the notch where the knob is now. A touch takes the nearer handle and drags it round the arc.
 * Tests find the ends by `<tag>_range_low` and `<tag>_range_high`.
 */
@Composable
internal fun LfoRange(
    control: Control,
    name: String,
    lfo: Lfo,
    value: Int,
    enabled: Boolean,
    onLfo: (Lfo) -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val current by rememberUpdatedState(lfo)
    val latest by rememberUpdatedState(onLfo)
    val low = CardWords.amount(control, lfo.low)
    val high = CardWords.amount(control, lfo.high)
    val described = stringResource(Res.string.lfo_range_what, name, low, high, CardWords.amount(control, value))
    Column(
        modifier.height(150.dp).padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
    ) {
        Box(Modifier.size(96.dp)) {
            Canvas(
                Modifier
                    .size(96.dp)
                    .semantics { contentDescription = described }
                    .pointerInput(enabled) {
                        if (!enabled) return@pointerInput
                        awaitEachGesture {
                            val down = awaitFirstDown()
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val grabbed = valueAt(down.position, center)
                            val takesLow = abs(grabbed - current.low) <= abs(grabbed - current.high)

                            fun move(to: Int) =
                                latest(if (takesLow) current.copy(low = to) else current.copy(high = to))
                            move(grabbed)
                            drag(down.id) { change ->
                                change.consume()
                                move(valueAt(change.position, center))
                            }
                        }
                    },
            ) {
                val stroke = 6.dp.toPx()
                val radius = size.minDimension / 2 - stroke
                val box = Size(radius * 2, radius * 2)
                val topLeft = Offset(center.x - radius, center.y - radius)
                drawArc(
                    colors.surfaceContainerHighest,
                    KnobDial.START_DEGREES,
                    KnobDial.SWEEP_DEGREES,
                    false,
                    topLeft,
                    box,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
                val from = minOf(lfo.low, lfo.high)
                val to = maxOf(lfo.low, lfo.high)
                drawArc(
                    colors.primary.copy(alpha = SWING_ALPHA),
                    KnobDial.angleDegrees(from),
                    KnobDial.angleDegrees(to) - KnobDial.angleDegrees(from),
                    false,
                    topLeft,
                    box,
                    style = Stroke(stroke),
                )
                val cap = radius * 22f / 32f
                drawCircle(colors.surfaceContainerHigh, cap, center)
                val notch = radians(value)
                drawLine(
                    if (enabled) colors.primary else colors.onSurfaceVariant,
                    Offset(center.x + cap * 0.15f * cos(notch), center.y + cap * 0.15f * sin(notch)),
                    Offset(center.x + cap * cos(notch), center.y + cap * sin(notch)),
                    3.5.dp.toPx(),
                    StrokeCap.Round,
                )
                listOf(lfo.low to false, lfo.high to true).forEach { (end, solid) ->
                    val at = radians(end)
                    val spot = Offset(center.x + radius * cos(at), center.y + radius * sin(at))
                    drawCircle(if (solid) colors.primary else colors.surface, 5.dp.toPx(), spot)
                    drawCircle(colors.primary, 5.dp.toPx(), spot, style = Stroke(2.dp.toPx()))
                }
            }
            End(stringResource(Res.string.lfo_range_low, name), lfo.low, enabled, "${tag}_range_low") {
                onLfo(lfo.copy(low = it))
            }
            End(stringResource(Res.string.lfo_range_high, name), lfo.high, enabled, "${tag}_range_high") {
                onLfo(lfo.copy(high = it))
            }
        }
        Text(
            stringResource(Res.string.lfo_range, low, high),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(name, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
    }
}

/** One end as its own node, so it can be read and set without dragging. */
@Composable
private fun End(
    description: String,
    value: Int,
    enabled: Boolean,
    tag: String,
    onValue: (Int) -> Unit,
) {
    Box(
        Modifier
            .size(96.dp)
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(value.toFloat(), 0f..KnobDial.MAX.toFloat(), KnobDial.MAX)
                if (enabled) {
                    setProgress { target ->
                        onValue(target.roundToInt().coerceIn(0, KnobDial.MAX))
                        true
                    }
                }
            }.testTag(tag),
    )
}

/** The knob value whose place on the arc is nearest [position]; past the gap at the bottom, the nearer end. */
private fun valueAt(
    position: Offset,
    center: Offset,
): Int {
    val degrees = (atan2(position.y - center.y, position.x - center.x) * 180 / PI).toFloat()
    val round = (degrees - KnobDial.START_DEGREES + 720) % 360
    if (round > KnobDial.SWEEP_DEGREES) return if (round > (KnobDial.SWEEP_DEGREES + 360) / 2) 0 else KnobDial.MAX
    return (round / KnobDial.SWEEP_DEGREES * KnobDial.MAX).roundToInt()
}

private fun radians(value: Int): Float = (KnobDial.angleDegrees(value) * PI / 180).toFloat()

// The swing is lit, not filled: the notch moving through it is what shows the LFO running.
private const val SWING_ALPHA = 0.35f
