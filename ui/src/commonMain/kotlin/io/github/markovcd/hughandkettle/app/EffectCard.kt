package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Sectors
import io.github.markovcd.hughandkettle.amp.Tempo
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.effect_collapse
import io.github.markovcd.hughandkettle.ui.effect_expand
import io.github.markovcd.hughandkettle.ui.effect_on
import io.github.markovcd.hughandkettle.ui.effect_switched_off
import io.github.markovcd.hughandkettle.ui.tempo_locked_at
import io.github.markovcd.hughandkettle.ui.time_tempo
import org.jetbrains.compose.resources.stringResource

/**
 * An effect's card: its name over a one-line summary and its switch, and while [open] and on, the
 * controls [behind] the switch. Closed, a card whose time is locked is outlined in amber and says so, even
 * with the effect off. Tests find the header by `effect:<id>`, the switch by `control:<id>` and the lock's
 * line by `locked:<id>`.
 */
@Composable
internal fun EffectCard(
    switch: Control,
    behind: List<Control>,
    panel: PanelState,
    enabled: Boolean,
    open: Boolean,
    onOpen: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = panel.state.values
    val on = switch.isOn(values[switch.id] ?: 0)
    val shown = open && on
    val toggle = stringResource(if (shown) Res.string.effect_collapse else Res.string.effect_expand, switch.name)
    val lock = panel.lock?.takeIf { held -> behind.any { it.controller == held.control.controller } }
    val locked = lock?.value?.takeIf { !shown }
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(16.dp),
        border = if (locked != null) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null,
    ) {
        Column(Modifier.padding(bottom = if (shown) 12.dp else 0.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier =
                        Modifier
                            .weight(1f)
                            .heightIn(min = 64.dp)
                            .clickable(enabled = on) { onOpen(!open) }
                            .semantics { contentDescription = toggle }
                            .padding(horizontal = 16.dp)
                            .testTag("effect:${switch.id}"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(switch.name, style = MaterialTheme.typography.titleMedium)
                        if (lock != null && locked != null) {
                            LockedLine(lock.control, locked, Modifier.testTag("locked:${switch.id}"))
                        } else if (!shown) {
                            Text(
                                summary(on, behind, values),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    if (on) Chevron(up = shown)
                }
                Switch(
                    checked = on,
                    onCheckedChange = {
                        panel.set(switch, if (it) 127 else 0)
                        onOpen(it)
                    },
                    enabled = enabled,
                    modifier =
                        Modifier
                            .padding(end = 16.dp)
                            .semantics { contentDescription = switch.name }
                            .testTag("control:${switch.id}"),
                )
            }
            if (shown) {
                // On the effect's own card its name need not lead every label: "Delay time" reads "Time".
                val named =
                    behind.map {
                        it.copy(
                            name = it.name.removePrefix("${switch.name} ").replaceFirstChar(Char::uppercase),
                        )
                    }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PanelRows.of(named).forEach { row ->
                        ControlRow(row, panel, enabled, Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }
    }
}

/** What a closed card says: off, else the first selector's pick or the first knob in a unit, else on. */
@Composable
private fun summary(
    on: Boolean,
    behind: List<Control>,
    values: Map<String, Int>,
): String {
    if (!on) return stringResource(Res.string.effect_switched_off)
    behind.forEach { control ->
        val value = values[control.id] ?: return@forEach
        val kind = control.kind
        val scale = control.scale
        if (kind is ControlKind.Selector) return kind.options[Sectors.index(value, kind.options.size)]
        if (scale != null) return timeWords(control, value)
    }
    return stringResource(Res.string.effect_on)
}

/** A knob's amount in its unit, and for a time knob its tempo too: "505 ms · 119 BPM". */
@Composable
private fun timeWords(
    control: Control,
    value: Int,
): String {
    val scale = control.scale ?: return "$value"
    val amount = scale.amount(value)
    val words = "$amount ${scale.unit}"
    return if (control.timed) stringResource(Res.string.time_tempo, words, "${Tempo.beatsPerMinute(amount)}") else words
}

/** A closed card's line while its time is locked at [value]: a padlock and "Tempo lock · 505 ms · 119 BPM". */
@Composable
private fun LockedLine(
    control: Control,
    value: Int,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val scale = control.scale ?: return
    val amount = scale.amount(value)
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Padlock(open = false, color = colors.primary, size = 14.dp)
        Text(
            stringResource(Res.string.tempo_locked_at, "$amount ${scale.unit}", "${Tempo.beatsPerMinute(amount)}"),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.onPrimaryContainer,
        )
    }
}

@Composable
private fun Chevron(up: Boolean) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(Modifier.size(20.dp)) {
        val u = size.width / 24
        val (tip, ends) = if (up) 9f to 15f else 15f to 9f
        drawLine(color, Offset(6 * u, ends * u), Offset(12 * u, tip * u), 2.5f * u, StrokeCap.Round)
        drawLine(color, Offset(12 * u, tip * u), Offset(18 * u, ends * u), 2.5f * u, StrokeCap.Round)
    }
}
