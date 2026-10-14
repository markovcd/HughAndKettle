package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import kotlin.time.Duration.Companion.nanoseconds

/**
 * One row of the panel: up to [PanelRows.KNOBS_PER_ROW] knobs sharing the width, or one switch or selector,
 * each described by its control's name and outlined while the amp has moved it.
 */
@Composable
internal fun ControlRow(
    row: List<Control>,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val first = row.first()
    val markColor = MaterialTheme.colorScheme.tertiary
    val markWords = stringResource(R.string.moved_on_amp)

    fun Modifier.marked(control: Control) = movedMark(control.id in panel.moved, markColor, markWords)
    when (val kind = first.kind) {
        ControlKind.Knob -> {
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { control ->
                    KnobCell(
                        control = control,
                        value = panel.state.values[control.id] ?: 0,
                        enabled = enabled,
                        onValue = { panel.set(control, it) },
                        modifier = Modifier.weight(1f).marked(control).testTag("control:${control.id}"),
                        onTap =
                            if (control.scale?.unit == "ms") {
                                ({ panel.tap(control, System.nanoTime().nanoseconds) })
                            } else {
                                null
                            },
                    )
                }
            }
        }

        ControlKind.Switch -> {
            SwitchRow(first, panel.state.values[first.id] ?: 0, enabled, {
                panel.set(first, it)
            }, modifier.marked(first).testTag("control:${first.id}"))
        }

        is ControlKind.Selector -> {
            SelectorRow(first, kind, panel.state.values[first.id] ?: 0, enabled, {
                panel.set(first, it)
            }, modifier.marked(first).testTag("control:${first.id}"))
        }
    }
}
