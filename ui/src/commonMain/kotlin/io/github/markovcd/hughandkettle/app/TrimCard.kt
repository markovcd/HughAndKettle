package io.github.markovcd.hughandkettle.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.trim_applied
import io.github.markovcd.hughandkettle.ui.trim_down
import io.github.markovcd.hughandkettle.ui.trim_up
import io.github.markovcd.hughandkettle.ui.volume_trim
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * The volume trim on a dial with zero at the top, stepped down and up. On, the card is outlined in amber; each
 * recall it trims lights the card and reads "Applied" for a second. Tests find the amount by `trim`, the
 * buttons by `trim:down` and `trim:up`.
 */
@Composable
internal fun TrimCard(
    trim: Int,
    trimmed: Int,
    enabled: Boolean,
    onTrim: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val on = trim != 0
    var applied by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(trimmed) }
    LaunchedEffect(trimmed) {
        if (trimmed == answered) return@LaunchedEffect
        answered = trimmed
        applied = true
        delay(1.seconds)
        applied = false
    }
    val ground by animateColorAsState(
        if (applied) colors.primary.copy(alpha = 0.1f) else colors.surfaceContainer,
        tween(if (applied) APPLIED_RISE_MS else APPLIED_FADE_MS),
    )
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ground)
            .border(1.dp, if (on) colors.primary else colors.outlineVariant, shape)
            .padding(start = 12.dp, end = 14.dp, top = 10.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TrimDial(trim, applied)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(Res.string.volume_trim).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontSize = 12.sp,
                    color = colors.onSurfaceVariant,
                )
                if (applied) {
                    Text(
                        stringResource(Res.string.trim_applied).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onPrimary,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(9.dp))
                                .background(colors.primary)
                                .padding(horizontal = 7.dp),
                    )
                }
            }
            Text(
                if (trim > 0) {
                    "+$trim"
                } else if (trim < 0) {
                    "−${-trim}"
                } else {
                    "0"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (on) colors.onPrimaryContainer else colors.onSurfaceVariant,
                modifier = Modifier.testTag("trim"),
            )
        }
        RoundButton(
            up = false,
            description = stringResource(Res.string.trim_down),
            enabled = enabled && trim > -KnobDial.MAX,
            onClick = { onTrim((trim - STEP).coerceAtLeast(-KnobDial.MAX)) },
            modifier = Modifier.testTag("trim:down"),
        )
        RoundButton(
            up = true,
            description = stringResource(Res.string.trim_up),
            enabled = enabled && trim < KnobDial.MAX,
            onClick = { onTrim((trim + STEP).coerceAtMost(KnobDial.MAX)) },
            modifier = Modifier.testTag("trim:up"),
        )
    }
}

/** A knob for -127..127 with zero at the top: an arc from the top to the amount, amber while on, ringed when [applied]. */
@Composable
private fun TrimDial(
    trim: Int,
    applied: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val track = colors.outlineVariant
    val ink = if (trim != 0) colors.primary else colors.onSurfaceVariant
    val tick = colors.onSurfaceVariant
    val cap = colors.surfaceContainerHigh
    val ring = colors.primary.copy(alpha = 0.55f)
    Canvas(Modifier.size(64.dp)) {
        val u = size.width / 80
        val center = Offset(40 * u, 40 * u)
        val arc = Size(64 * u, 64 * u)
        val corner = Offset(8 * u, 8 * u)
        val stroke = Stroke(6 * u, cap = StrokeCap.Round)
        val turn = HALF_SWEEP * trim / KnobDial.MAX
        if (applied) drawCircle(ring, 38 * u, center, style = Stroke(2 * u))
        drawArc(track, 135f, 270f, false, corner, arc, style = stroke)
        if (trim != 0) drawArc(colors.primary, -90f, turn, false, corner, arc, style = stroke)
        if (!applied) drawLine(tick, Offset(40 * u, 0.5f * u), Offset(40 * u, 4 * u), 2 * u, StrokeCap.Round)
        drawCircle(cap, 22 * u, center)
        rotate(
            turn,
            center,
        ) { drawLine(ink, Offset(40 * u, 21 * u), Offset(40 * u, 31 * u), 3.5f * u, StrokeCap.Round) }
    }
}

/** How far one press moves the trim, in the knob's 0..127 steps. */
private const val STEP = 5

/** How far round the dial turns either way from the top at the most. */
private const val HALF_SWEEP = 135f

// The lit card rises at once and fades back over most of the second it is lit.
private const val APPLIED_RISE_MS = 120
private const val APPLIED_FADE_MS = 900
