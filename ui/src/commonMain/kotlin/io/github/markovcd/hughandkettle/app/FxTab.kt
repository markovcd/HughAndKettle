package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Section

/**
 * The effects, one card each under the dice that randomizes them: an effect with controls behind its switch
 * opens to show them, one at a time and only while it is on; switching one on opens it. A switch with nothing
 * behind it is a plain card. After a randomize, a bar over the bottom offers its undo.
 */
@Composable
fun FxTab(
    panel: PanelState,
    songs: SongState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val fx = panel.amp.controls.filter { it.section == Section.Fx }
    val rolled = panel.rolled
    Box(modifier) {
        Column(
            // Room under the last card, so the undo bar never covers it.
            Modifier.verticalScroll(rememberScrollState()).padding(12.dp).padding(
                bottom =
                    if (rolled !=
                        null
                    ) {
                        64.dp
                    } else {
                        0.dp
                    },
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RandomizeCard(panel, enabled)
            SwitchCards(fx.filter { it.kind == ControlKind.Switch }, fx, panel, songs, enabled)
        }
        if (rolled != null) {
            UndoBar(rolled, panel::undoRoll, Modifier.align(Alignment.BottomCenter).padding(12.dp))
        }
    }
}
