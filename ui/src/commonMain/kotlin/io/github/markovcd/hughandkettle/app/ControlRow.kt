package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.moved_on_amp
import org.jetbrains.compose.resources.stringResource

/**
 * One row of the panel: up to [PanelRows.KNOBS_PER_ROW] knobs sharing the width, or one switch or selector, each described by its control's name and outlined while the amp has moved it.
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
    val markWords = stringResource(Res.string.moved_on_amp)

    fun Modifier.marked(control: Control) = movedMark(control.id in panel.moved, markColor, markWords)
    when (val kind = first.kind) {
        ControlKind.Knob -> {
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { control ->
                    val cell = Modifier.weight(1f).marked(control).testTag("control:${control.id}")
                    KnobCell(
                        control,
                        panel.state.values[control.id] ?: 0,
                        enabled,
                        { panel.set(control, it) },
                        cell,
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
