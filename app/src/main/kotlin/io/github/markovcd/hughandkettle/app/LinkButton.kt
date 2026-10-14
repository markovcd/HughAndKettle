package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.link
import org.jetbrains.compose.resources.stringResource

/**
 * The link's state as a glyph, which opens the [LinkCard] while there is a link. While there is none the
 * card is on the panel itself, so the dialog closes. Tests find it by `link` and the dialog by `link_dialog`.
 */
@Composable
internal fun LinkButton(
    panel: PanelState,
    discovery: DiscoveryState,
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val words = stringResource(Res.string.link)
    IconButton(
        onClick = { open = true },
        modifier = Modifier.testTag("link").semantics { contentDescription = words },
    ) {
        LinkGlyph(panel)
    }
    if (open && !panel.idle) {
        Dialog(onDismissRequest = { open = false }) {
            LinkCard(panel, discovery, Modifier.padding(8.dp).testTag("link_dialog"))
        }
    }
}
