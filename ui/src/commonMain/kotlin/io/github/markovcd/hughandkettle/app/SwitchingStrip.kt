package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import io.github.markovcd.hughandkettle.amp.Sectors

/**
 * The switching, always in view: each selector as one row of segments with no title, as the channels,
 * and each switch as a chip with a lit dot, as boost.
 */
@Composable
fun SwitchingStrip(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        panel.amp.controls.filter { it.section == Section.Switching }.forEach { control ->
            val value = panel.state.values[control.id] ?: 0
            val tagged = Modifier.testTag("control:${control.id}")
            when (val kind = control.kind) {
                is ControlKind.Selector -> {
                    val count = kind.options.size
                    val picked = Sectors.index(value, count)
                    SingleChoiceSegmentedButtonRow(
                        Modifier
                            .weight(1f)
                            .height(48.dp)
                            .semantics { contentDescription = control.name }
                            .then(tagged),
                    ) {
                        kind.options.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = index == picked,
                                onClick = {
                                    if (index != picked) {
                                        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
                                        panel.set(control, Sectors.value(index, count))
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = count),
                                enabled = enabled,
                                icon = {},
                                colors =
                                    SegmentedButtonDefaults.colors(
                                        activeContainerColor = colors.primary,
                                        activeContentColor = colors.onPrimary,
                                    ),
                            ) {
                                Text(option, maxLines = 1)
                            }
                        }
                    }
                }

                ControlKind.Switch -> {
                    val on = control.isOn(value)
                    FilterChip(
                        selected = on,
                        onClick = { panel.set(control, if (on) 0 else 127) },
                        enabled = enabled,
                        label = { Text(control.name) },
                        leadingIcon = {
                            Box(
                                Modifier
                                    .size(
                                        8.dp,
                                    ).background(if (on) colors.onPrimary else colors.outline, CircleShape),
                            )
                        },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = colors.primary,
                                selectedLabelColor = colors.onPrimary,
                            ),
                        modifier = Modifier.height(48.dp).then(tagged),
                    )
                }

                ControlKind.Knob -> {
                    KnobCell(control, value, enabled, { panel.set(control, it) }, tagged)
                }
            }
        }
    }
}
