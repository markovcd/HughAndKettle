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

/**
 * The effects, one card each: an effect with controls behind its switch opens to show them, one at a
 * time and only while it is on; switching one on opens it. A switch with nothing behind it is a plain card.
 */
@Composable
fun FxTab(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val fx = panel.amp.controls.filter { it.section == Section.Fx }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SwitchCards(fx.filter { it.kind == ControlKind.Switch }, fx, panel, enabled)
    }
}
