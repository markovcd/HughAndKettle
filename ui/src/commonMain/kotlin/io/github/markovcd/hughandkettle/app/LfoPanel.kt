package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.LfoShape
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.lfo
import io.github.markovcd.hughandkettle.ui.lfo_crosses
import io.github.markovcd.hughandkettle.ui.lfo_hz
import io.github.markovcd.hughandkettle.ui.lfo_moves
import io.github.markovcd.hughandkettle.ui.lfo_rate
import io.github.markovcd.hughandkettle.ui.lfo_rates
import io.github.markovcd.hughandkettle.ui.lfo_shape
import io.github.markovcd.hughandkettle.ui.lfo_shape_ramp_down
import io.github.markovcd.hughandkettle.ui.lfo_shape_ramp_up
import io.github.markovcd.hughandkettle.ui.lfo_shape_random
import io.github.markovcd.hughandkettle.ui.lfo_shape_sine
import io.github.markovcd.hughandkettle.ui.lfo_shape_square
import io.github.markovcd.hughandkettle.ui.lfo_shape_triangle
import io.github.markovcd.hughandkettle.ui.lfo_stepped
import io.github.markovcd.hughandkettle.ui.lfo_time_free
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The LFO on an effect card: its switch and, while on, what it moves, its shape and its rate, with a line for
 * a time it took from the tempo lock and for a selector swing that crosses into another option. Tests find
 * the switch by `fx_lfo_<effect>`, the rest by `fx_lfo_<effect>_target:<control>`, `_shape:<shape>` and `_hz`.
 */
@Composable
internal fun LfoPanel(
    switch: Control,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val lfos = panel.lfos
    val lfo = lfos.of(switch) ?: return
    val on = switch.id in lfos.running
    val tag = "fx_lfo_${switch.id}"
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, if (on) colors.primary.copy(alpha = 0.4f) else colors.outlineVariant),
    ) {
        Column(
            Modifier.padding(start = 12.dp, end = 10.dp, bottom = if (on) 12.dp else 0.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                LfoGlyph(LfoShape.Sine, if (on) colors.primary else colors.onSurfaceVariant)
                Text(
                    stringResource(Res.string.lfo),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                val name = stringResource(Res.string.lfo)
                Switch(
                    checked = on,
                    onCheckedChange = { lfos.switch(switch, it) },
                    enabled = enabled,
                    modifier = Modifier.semantics { contentDescription = name }.testTag(tag),
                )
            }
            if (!on) return@Column
            val target = CardWords.short(switch, lfo.control)
            if (lfo.control.timed) Notice(stringResource(Res.string.lfo_time_free, target))
            val selector = lfo.control.kind as? ControlKind.Selector
            if (selector != null && lfo.crosses) {
                val (from, to) = listOf(lfo.low, lfo.high).sorted().map { CardWords.option(selector, it) }
                Notice(stringResource(Res.string.lfo_crosses, target, from, to))
            }
            val targets = lfos.targets(switch)
            if (targets.size > 1) {
                Labeled(Res.string.lfo_moves) {
                    Segments(targets, { it.id == lfo.control.id }, enabled, { lfos.aim(switch, it) }, {
                        "${tag}_target:${it.id.removePrefix("${switch.id}_")}"
                    }) { Text(CardWords.short(switch, it)) }
                }
            }
            Labeled(Res.string.lfo_shape) {
                Segments(LfoShape.entries, { it == lfo.shape }, enabled, { lfos.set(switch, lfo.copy(shape = it)) }, {
                    "${tag}_shape:${it.id}"
                }, { stringResource(it.words()) }) { shape ->
                    LfoGlyph(shape, if (shape == lfo.shape) colors.onPrimary else colors.onSurface)
                }
            }
            Labeled(Res.string.lfo_rate) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val rate = stringResource(Res.string.lfo_rate)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Knob(
                            value = LfoDial.value(lfo.hz),
                            onValue = { lfos.set(switch, lfo.copy(hz = LfoDial.hz(it))) },
                            description = rate,
                            enabled = enabled,
                            size = 60.dp,
                            modifier = Modifier.testTag("${tag}_hz"),
                        )
                        Text(
                            stringResource(Res.string.lfo_hz, CardWords.hz(lfo.hz)),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.testTag("${tag}_hz_value"),
                        )
                    }
                    val rates = stringResource(Res.string.lfo_rates)
                    val stepped = stringResource(Res.string.lfo_stepped)
                    Text(
                        if (lfo.stepped) "$rates $stepped" else rates,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
}

@Composable
private fun Labeled(
    label: StringResource,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(label).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
    }
}

/** One choice of [items] as a row of segments, each found by [tagOf] and read by [described] when it draws no words. */
@Composable
private fun <T> Segments(
    items: List<T>,
    picked: (T) -> Boolean,
    enabled: Boolean,
    onPick: (T) -> Unit,
    tagOf: (T) -> String,
    described: @Composable (T) -> String? = { null },
    label: @Composable (T) -> Unit,
) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(44.dp)) {
        items.forEachIndexed { index, item ->
            val words = described(item)
            SegmentedButton(
                selected = picked(item),
                onClick = { onPick(item) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = items.size),
                enabled = enabled,
                icon = {},
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.primary,
                        activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                modifier =
                    Modifier
                        .testTag(tagOf(item))
                        .then(if (words != null) Modifier.semantics { contentDescription = words } else Modifier),
            ) {
                label(item)
            }
        }
    }
}

private fun LfoShape.words(): StringResource =
    when (this) {
        LfoShape.Sine -> Res.string.lfo_shape_sine
        LfoShape.Triangle -> Res.string.lfo_shape_triangle
        LfoShape.Square -> Res.string.lfo_shape_square
        LfoShape.RampUp -> Res.string.lfo_shape_ramp_up
        LfoShape.RampDown -> Res.string.lfo_shape_ramp_down
        LfoShape.Random -> Res.string.lfo_shape_random
    }
