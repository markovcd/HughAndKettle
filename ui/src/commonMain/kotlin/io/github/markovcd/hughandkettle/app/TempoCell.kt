package io.github.markovcd.hughandkettle.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.Tempo
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.bpm
import io.github.markovcd.hughandkettle.ui.tap
import io.github.markovcd.hughandkettle.ui.tempo_held
import io.github.markovcd.hughandkettle.ui.tempo_lock
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A time knob and its tempo: the knob and its time on the left; the beats per minute, tap tempo and the
 * tempo lock on the right. Locked, the area is outlined in amber with a padlock on the knob; each preset
 * the lock answers lights it and reads "Held" for a second. A time the amp has not reported reads "—".
 * Tests find the time by `value:<id>`, the tap button by `tap:<id>` and the lock by `lock:<id>`.
 */
@Composable
internal fun TempoCell(
    control: Control,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scale = control.scale ?: return
    val value = panel.state.values[control.id]
    val ms = value?.let(scale::amount)
    val locked = panel.lock?.control?.controller == control.controller
    var held by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(panel.restored) }
    LaunchedEffect(panel.restored) {
        if (panel.restored == answered) return@LaunchedEffect
        answered = panel.restored
        held = true
        delay(1.seconds)
        held = false
    }
    val outline by animateColorAsState(if (locked) colors.primary else colors.outlineVariant, tween(HELD_FADE_MS))
    val ground by animateColorAsState(
        if (held) colors.primary.copy(alpha = 0.14f) else colors.surfaceContainerLow,
        tween(if (held) HELD_RISE_MS else HELD_FADE_MS),
    )
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ground)
            .border(if (held) 2.dp else 1.dp, outline, shape)
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                Knob(value ?: 0, { panel.set(control, it) }, control.name, enabled = enabled, size = 72.dp)
                if (locked) {
                    Box(
                        Modifier.offset(y = 9.dp),
                    ) { Padlock(open = false, color = colors.onPrimary, size = 16.dp, solid = true) }
                }
            }
            Text(
                ms?.let { "$it ${scale.unit}" } ?: UNKNOWN,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                modifier = Modifier.testTag("value:${control.id}"),
            )
            Text(
                control.name.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    ms?.let { "${Tempo.beatsPerMinute(it)}" } ?: UNKNOWN,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("bpm:${control.id}"),
                )
                Text(
                    stringResource(Res.string.bpm),
                    style = MaterialTheme.typography.labelLarge,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 5.dp),
                )
            }
            OutlinedButton(
                onClick = { panel.tap(control, System.nanoTime().nanoseconds) },
                enabled = enabled,
                shape = RoundedCornerShape(12.dp),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        containerColor = colors.surfaceContainerHigh,
                        contentColor = colors.primary,
                    ),
                modifier = Modifier.fillMaxWidth().height(44.dp).testTag("tap:${control.id}"),
            ) {
                Text(
                    stringResource(Res.string.tap),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
            LockButton(locked, held, enabled, { panel.lock(control, it) }, Modifier.testTag("lock:${control.id}"))
        }
    }
}

/** The tempo lock as a toggle button: an open grey padlock while off, amber while on, solid amber reading "Held" when [held]. */
@Composable
private fun LockButton(
    locked: Boolean,
    held: Boolean,
    enabled: Boolean,
    onLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val ground =
        when {
            held -> colors.primary
            locked -> colors.primary.copy(alpha = 0.16f)
            else -> colors.surfaceContainer
        }
    val ink =
        when {
            held -> colors.onPrimary
            locked -> colors.primary
            else -> colors.onSurfaceVariant
        }
    val label =
        when {
            held -> colors.onPrimary
            locked -> colors.onPrimaryContainer
            else -> colors.onSurface
        }
    Row(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(ground)
            .border(1.dp, if (locked) colors.primary else colors.outlineVariant, shape)
            .toggleable(value = locked, enabled = enabled, role = Role.Switch, onValueChange = onLock),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Padlock(open = !locked, color = ink)
        Text(
            stringResource(if (held) Res.string.tempo_held else Res.string.tempo_lock),
            style = MaterialTheme.typography.labelLarge,
            color = label,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val UNKNOWN = "—"

// The lit area rises at once and fades back over most of the second it is held.
private const val HELD_RISE_MS = 120
private const val HELD_FADE_MS = 900
