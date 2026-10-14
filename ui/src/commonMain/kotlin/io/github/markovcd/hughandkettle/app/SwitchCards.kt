package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.Control

/**
 * A card per switch: one with controls behind it among [controls] opens to show them, one at a time and only
 * while it is on, and switching it on opens it. A switch with nothing behind it is a plain card.
 */
@Composable
internal fun SwitchCards(
    switches: List<Control>,
    controls: List<Control>,
    panel: PanelState,
    enabled: Boolean,
) {
    var open by rememberSaveable { mutableStateOf<String?>(null) }
    switches.forEach { switch ->
        val behind = controls.filter { it.needs == switch.id }
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
