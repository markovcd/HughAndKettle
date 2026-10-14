package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Section

/**
 * The whole screen: a top bar of the preset, the link's glyph, the header's switches and the settings, where
 * the amp is picked; the switching, always in view; the link card while there is no link; then one tab of
 * the amp, its effects or the tones, picked along the bottom. A box linked to with no amp picked for it asks
 * which amp it is. Wider than [TWO_PANES_FROM], as a phone or a tablet in landscape, the tabs move to a rail
 * at the side and the amp and its effects share a tab side by side. Tests find the tab's content by `panel`,
 * the effects beside the amp by `controls`, and a control by `control:<id>`.
 */
@Composable
fun AmpPanel(
    panel: PanelState,
    discovery: DiscoveryState,
    shelf: ToneShelf,
    presetNames: PresetNamesState,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
) {
    // With no link there is nothing to turn; the controls dim to 38% and the link card keeps full contrast.
    val enabled = panel.linked
    val dim = Modifier.alpha(if (enabled) 1f else DIMMED)
    var picked by rememberSaveable { mutableStateOf(PanelTab.Amp) }
    if (panel.asking) AmpQuestion(panel)
    BoxWithConstraints {
        val wide = maxWidth >= TWO_PANES_FROM
        val tabs = if (wide) listOf(PanelTab.Amp, PanelTab.Tones) else PanelTab.entries
        val tab = if (picked in tabs) picked else PanelTab.Amp
        val body: @Composable ColumnScope.() -> Unit = {
            Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    PresetRow(panel, presetNames, enabled, Modifier.widthIn(max = PRESET_WIDTH).then(dim))
                }
                LinkButton(panel, discovery)
                panel.amp.controls.filter { it.section == Section.Header }.forEach {
                    HeaderSwitch(it, panel, enabled, dim)
                }
                SettingsMenu(panel, theme, onTheme)
            }
            SwitchingStrip(panel, enabled, dim)
            if (panel.idle) LinkCard(panel, discovery, Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
            val content = Modifier.weight(1f).fillMaxWidth()
            when {
                tab == PanelTab.Tones -> {
                    Column(content.verticalScroll(rememberScrollState()).padding(12.dp).testTag("panel")) {
                        ToneShelfCard(shelf, enabled)
                    }
                }

                wide -> {
                    Row(content) {
                        AmpTab(panel, enabled, Modifier.weight(1f).then(dim).testTag("panel"))
                        FxTab(panel, enabled, Modifier.weight(1f).then(dim).testTag("controls"))
                    }
                }

                tab == PanelTab.Amp -> {
                    AmpTab(panel, enabled, content.then(dim).testTag("panel"))
                }

                else -> {
                    FxTab(panel, enabled, content.then(dim).testTag("panel"))
                }
            }
        }
        if (wide) {
            Row(Modifier.fillMaxSize()) {
                NavigationRail {
                    tabs.forEach {
                        NavigationRailItem(
                            selected = it == tab,
                            onClick = { picked = it },
                            icon = { TabGlyph(it, selected = it == tab) },
                            label = { Text(stringResource(it.label)) },
                            modifier = Modifier.testTag("tab:${it.id}"),
                        )
                    }
                }
                Column(Modifier.weight(1f).fillMaxHeight(), content = body)
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                body()
                NavigationBar {
                    tabs.forEach {
                        NavigationBarItem(
                            selected = it == tab,
                            onClick = { picked = it },
                            icon = { TabGlyph(it, selected = it == tab) },
                            label = { Text(stringResource(it.label)) },
                            modifier = Modifier.testTag("tab:${it.id}"),
                        )
                    }
                }
            }
        }
    }
}

private const val DIMMED = 0.38f

// Material's expanded width: a phone or a tablet in landscape, or an unfolded phone.
private val TWO_PANES_FROM = 840.dp
private val PRESET_WIDTH = 280.dp
