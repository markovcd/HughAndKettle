package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import io.github.markovcd.hughandkettle.wmi.LinkState

/**
 * The whole screen: the amp's name, the link card, then the controls by section. Knobs sit three to a
 * row, switches and selectors take a row each. Tests find a control's row by `control:<id>`.
 */
@Composable
fun AmpPanel(
    panel: PanelState,
    discovery: DiscoveryState,
) {
    // With no link there is nothing to turn; the rows dim to 38% and the link card keeps full contrast.
    val enabled = panel.link is LinkState.Connected
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(
                Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(panel.amp.name, style = MaterialTheme.typography.titleLarge)
            }
        }
        item { LinkCard(panel, discovery, Modifier.padding(horizontal = 12.dp)) }
        item { PresetRow(panel, enabled, Modifier.padding(horizontal = 12.dp).alpha(if (enabled) 1f else DIMMED)) }
        Section.entries.forEach { section ->
            val controls = panel.amp.controls.filter { it.section == section }
            if (controls.isEmpty()) return@forEach
            item(key = "section:$section") { SectionLabel(section) }
            PanelRows.of(controls).forEach { row ->
                item(key = row.joinToString(",") { it.id }) {
                    ControlRow(
                        row,
                        panel,
                        enabled,
                        Modifier.padding(horizontal = 12.dp).alpha(if (enabled) 1f else DIMMED),
                    )
                }
            }
        }
    }
}

@Composable
private fun ControlRow(
    row: List<Control>,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val first = row.first()
    when (val kind = first.kind) {
        ControlKind.Knob -> {
            Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { control ->
                    KnobCell(
                        control = control,
                        value = panel.state.values[control.id] ?: 0,
                        enabled = enabled,
                        onValue = { panel.set(control, it) },
                        modifier = Modifier.weight(1f).testTag("control:${control.id}"),
                    )
                }
                repeat(PanelRows.KNOBS_PER_ROW - row.size) { Box(Modifier.weight(1f)) }
            }
        }

        ControlKind.Switch -> {
            SwitchRow(first, panel.state.values[first.id] ?: 0, enabled, {
                panel.set(first, it)
            }, modifier.testTag("control:${first.id}"))
        }

        is ControlKind.Selector -> {
            SelectorRow(first, kind, panel.state.values[first.id] ?: 0, enabled, {
                panel.set(first, it)
            }, modifier.testTag("control:${first.id}"))
        }
    }
}

private const val DIMMED = 0.38f
