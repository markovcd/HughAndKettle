package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The whole screen: the amp picker and the theme button, the link card, the preset and the tones, then
 * the controls by section, knobs several to a row and switches and selectors a row each. Wider than
 * [TWO_PANES_FROM], as a tablet in landscape, the controls get a pane of their own to the right with
 * six knobs to a row. Tests find the panes by `panel` and `controls`, and a control's row by `control:<id>`.
 */
@Composable
fun AmpPanel(
    panel: PanelState,
    discovery: DiscoveryState,
    shelf: ToneShelf,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
) {
    // With no link there is nothing to turn; the rows dim to 38% and the link card keeps full contrast.
    val enabled = panel.linked
    BoxWithConstraints {
        if (maxWidth < TWO_PANES_FROM) {
            LazyColumn(contentPadding = PaddingValues(bottom = 24.dp), modifier = Modifier.testTag("panel")) {
                head(panel, discovery, shelf, theme, onTheme, enabled)
                controls(panel, enabled, PanelRows.KNOBS_PER_ROW)
            }
        } else {
            Row {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 24.dp),
                    modifier = Modifier.width(HEAD_PANE_WIDTH).testTag("panel"),
                ) {
                    head(panel, discovery, shelf, theme, onTheme, enabled)
                }
                LazyColumn(
                    contentPadding = PaddingValues(top = 8.dp, bottom = 24.dp),
                    modifier = Modifier.weight(1f).testTag("controls"),
                ) {
                    controls(panel, enabled, WIDE_KNOBS_PER_ROW)
                }
            }
        }
    }
}

private fun LazyListScope.head(
    panel: PanelState,
    discovery: DiscoveryState,
    shelf: ToneShelf,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
    enabled: Boolean,
) {
    item {
        Row(
            Modifier.fillMaxWidth().height(64.dp).padding(start = 4.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AmpPicker(panel, Modifier.weight(1f))
            TextButton(onClick = { onTheme(theme.next) }, modifier = Modifier.testTag("theme")) {
                Text(stringResource(theme.label))
            }
        }
    }
    item { LinkCard(panel, discovery, Modifier.padding(horizontal = 12.dp)) }
    item { PresetRow(panel, enabled, Modifier.padding(horizontal = 12.dp).alpha(if (enabled) 1f else DIMMED)) }
    item { ToneShelfCard(shelf, enabled, Modifier.padding(horizontal = 12.dp)) }
}

private fun LazyListScope.controls(
    panel: PanelState,
    enabled: Boolean,
    knobsPerRow: Int,
) {
    Section.entries.forEach { section ->
        val controls = panel.amp.controls.filter { it.section == section }
        if (controls.isEmpty()) return@forEach
        item(key = "section:$section") { SectionLabel(section) }
        PanelRows.of(controls, knobsPerRow).forEach { row ->
            item(key = row.joinToString(",") { it.id }) {
                ControlRow(
                    row,
                    panel,
                    enabled,
                    Modifier.padding(horizontal = 12.dp).alpha(if (enabled) 1f else DIMMED),
                    knobsPerRow,
                )
            }
        }
    }
}

/**
 * One row of the panel: up to [knobsPerRow] knobs, or one switch or selector, each described by its
 * control's name, dimmed while its effect is off and outlined while the amp has moved it.
 */
@Composable
internal fun ControlRow(
    row: List<Control>,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    knobsPerRow: Int = PanelRows.KNOBS_PER_ROW,
) {
    val first = row.first()
    val markColor = MaterialTheme.colorScheme.tertiary
    val markWords = stringResource(R.string.moved_on_amp)
    val offWords = stringResource(R.string.effect_off)

    fun Modifier.marked(control: Control) =
        effectOff(panel.state.silent(control), offWords).movedMark(control.id in panel.moved, markColor, markWords)
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
                            if (control.scale?.unit ==
                                "ms"
                            ) {
                                ({ panel.tap(control, System.nanoTime().nanoseconds) })
                            } else {
                                null
                            },
                    )
                }
                repeat(knobsPerRow - row.size) { Box(Modifier.weight(1f)) }
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

private const val DIMMED = 0.38f

// Material's expanded width: a tablet in landscape, or an unfolded phone.
private val TWO_PANES_FROM = 840.dp
private val HEAD_PANE_WIDTH = 400.dp
private const val WIDE_KNOBS_PER_ROW = 6
