package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Sectors

/** Up to four options as one segmented row; more as chips, three to a row. A sector change ticks. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SelectorRow(
    control: Control,
    selector: ControlKind.Selector,
    value: Int,
    enabled: Boolean,
    onValue: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val count = selector.options.size
    val picked = Sectors.index(value, count)

    fun pick(index: Int) {
        if (index == picked) return
        haptics.performHapticFeedback(HapticFeedbackType.SegmentTick)
        onValue(Sectors.value(index, count))
    }
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(control.name, style = MaterialTheme.typography.labelLarge)
            Text(selector.options[picked], style = MaterialTheme.typography.headlineSmall)
        }
        if (count <= SEGMENTS_AT_MOST) {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().height(56.dp)) {
                selector.options.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = index == picked,
                        onClick = { pick(index) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = count),
                        enabled = enabled,
                        icon = {},
                        colors =
                            SegmentedButtonDefaults.colors(
                                activeContainerColor = MaterialTheme.colorScheme.primary,
                                activeContentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text(option)
                    }
                }
            }
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                maxItemsInEachRow = 3,
            ) {
                selector.options.forEachIndexed { index, option ->
                    FilterChip(
                        selected = index == picked,
                        onClick = { pick(index) },
                        enabled = enabled,
                        label = { Text(option) },
                        modifier = Modifier.weight(1f).height(48.dp),
                    )
                }
            }
        }
    }
}

private const val SEGMENTS_AT_MOST = 4
