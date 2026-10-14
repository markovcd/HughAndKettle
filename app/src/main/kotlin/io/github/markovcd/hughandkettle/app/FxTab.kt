package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section

/**
 * The effects, one card each: an effect with controls behind its switch opens to show them, one at a
 * time and only while it is on; switching one on opens it. A switch with nothing behind it is a plain card.
 */
@Composable
internal fun FxTab(
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    val fx = panel.amp.controls.filter { it.section == Section.Fx }
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        fx.filter { it.kind == ControlKind.Switch }.forEach { switch ->
            val behind = fx.filter { it.needs == switch.id }
            if (behind.isEmpty()) {
                SwitchCard(switch, panel, enabled)
            } else {
                EffectCard(
                    switch = switch,
                    behind = behind,
                    panel = panel,
                    enabled = enabled,
                    open = open == switch.id,
                    onOpen = { open = if (it) switch.id else null },
                )
            }
        }
    }
}
