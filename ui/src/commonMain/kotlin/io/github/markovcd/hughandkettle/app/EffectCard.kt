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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Lfo
import io.github.markovcd.hughandkettle.amp.Section
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.effect_collapse
import io.github.markovcd.hughandkettle.ui.effect_expand
import io.github.markovcd.hughandkettle.ui.effect_on
import io.github.markovcd.hughandkettle.ui.effect_switched_off
import io.github.markovcd.hughandkettle.ui.lfo_after
import io.github.markovcd.hughandkettle.ui.lfo_hz
import io.github.markovcd.hughandkettle.ui.lfo_running
import io.github.markovcd.hughandkettle.ui.lfo_waiting
import io.github.markovcd.hughandkettle.ui.tempo_locked_at
import org.jetbrains.compose.resources.stringResource

/**
 * An effect's card: its name over a one-line summary and its switch, and while [open] and on, the
 * controls [behind] the switch; a time among them sits under its tempo lock, and gives way to the notes
 * while locked. Closed, a card whose time is locked is outlined in amber and says so, even with the effect
 * off. Under the controls sits the card's LFO; the control it sweeps shows its swing, and a closed card
 * says the LFO runs, or waits for the effect. Tests find the header by `effect:<id>`, the switch by
 * `control:<id>`, the lock's line by `locked:<id>` and the LFO's by `lfo_line:<id>`.
 */
@Composable
internal fun EffectCard(
    switch: Control,
    behind: List<Control>,
    panel: PanelState,
    songs: SongState,
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
    val lfo = panel.lfos.running[switch.id]
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
                        if (lfo != null && !shown) {
                            LfoLine(switch, lfo, on, detail(behind, values), Modifier.testTag("lfo_line:${switch.id}"))
                        } else if (lock != null && locked != null) {
                            LockedLine(songs, Modifier.testTag("locked:${switch.id}"))
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
                val named = behind.map { it.copy(name = CardWords.short(switch, it)) }
                val timed = behind.any(Control::timed)
                if (timed) DelayTempo(songs, panel, enabled, Modifier.padding(horizontal = 12.dp))
                val knobs = if (timed && songs.timed) named.filterNot(Control::timed) else named
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    PanelRows.of(knobs).forEach { row ->
                        if (lfo != null && row.any { it.id == lfo.control.id }) {
                            LfoRow(row, switch, lfo, panel, enabled, Modifier.padding(horizontal = 4.dp))
                        } else {
                            ControlRow(row, panel, enabled, Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
                if (switch.section == Section.Fx) LfoPanel(switch, panel, enabled, Modifier.padding(horizontal = 12.dp))
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
    return detail(behind, values) ?: stringResource(Res.string.effect_on)
}

/** The first selector's pick or the first knob in a unit, as a closed card names its effect; null for neither. */
private fun detail(
    behind: List<Control>,
    values: Map<String, Int>,
): String? {
    behind.forEach { control ->
        val value = values[control.id] ?: return@forEach
        val kind = control.kind
        val scale = control.scale
        if (kind is ControlKind.Selector) return CardWords.option(kind, value)
        if (scale != null) return CardWords.amount(control, value)
    }
    return null
}

/**
 * A closed card's line while its LFO runs: its wave and "LFO · Volume · 0.5 Hz"; with the effect off,
 * "Chorus · LFO waiting: Modulation is off".
 */
@Composable
private fun LfoLine(
    switch: Control,
    lfo: Lfo,
    on: Boolean,
    detail: String?,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val color = if (on) colors.primary else colors.error
    val text =
        if (on) {
            stringResource(
                Res.string.lfo_running,
                CardWords.short(switch, lfo.control),
                stringResource(Res.string.lfo_hz, CardWords.hz(lfo.hz)),
            )
        } else {
            val waiting = stringResource(Res.string.lfo_waiting, switch.name)
            if (detail == null) waiting else stringResource(Res.string.lfo_after, detail, waiting)
        }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        if (on) LfoGlyph(lfo.shape, color, Modifier.size(18.dp, 11.dp))
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A row of knobs, or a selector, one of which an LFO sweeps: that one shows its swing, the rest turn as ever. */
@Composable
private fun LfoRow(
    row: List<Control>,
    switch: Control,
    lfo: Lfo,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
        row.forEach { control ->
            val value = panel.state.values[control.id] ?: 0
            if (control.id == lfo.control.id) {
                LfoRange(
                    lfo.control,
                    control.name,
                    lfo,
                    value,
                    enabled,
                    { panel.lfos.set(switch, it) },
                    "fx_lfo_${switch.id}",
                    Modifier.weight(1f).testTag("control:${control.id}"),
                )
            } else {
                KnobCell(
                    control,
                    value,
                    enabled,
                    { panel.set(control, it) },
                    Modifier.weight(1f).testTag("control:${control.id}"),
                )
            }
        }
    }
}

/** A closed card's line while its time is locked: a padlock and "Tempo lock · Dotted 1/8 · 120 BPM". */
@Composable
private fun LockedLine(
    songs: SongState,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        Padlock(open = false, color = colors.primary, size = 14.dp)
        Text(
            stringResource(Res.string.tempo_locked_at, NoteWords.phrase(songs.sounding), "${songs.song.tempo}"),
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
