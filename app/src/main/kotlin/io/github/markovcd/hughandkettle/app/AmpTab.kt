package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section

/** The amp's own controls by section: each section's knobs, then its selectors, then its switches on cards. */
@Composable
internal fun AmpTab(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier.verticalScroll(rememberScrollState()).padding(bottom = 12.dp)) {
        listOf(Section.Preamp, Section.PowerAmp).forEach { section ->
            val controls = panel.amp.controls.filter { it.section == section }
            if (controls.isEmpty()) return@forEach
            SectionLabel(section)
            val (switches, others) = controls.partition { it.kind == ControlKind.Switch }
            PanelRows.of(others.sortedBy { it.kind is ControlKind.Selector }).forEach { row ->
                ControlRow(row, panel, enabled, Modifier.padding(horizontal = 12.dp))
            }
            Column(
                Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                switches.forEach { SwitchCard(it, panel, enabled) }
            }
        }
    }
}
