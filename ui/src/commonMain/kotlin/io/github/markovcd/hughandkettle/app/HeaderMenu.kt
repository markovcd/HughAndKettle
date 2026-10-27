package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.amp
import io.github.markovcd.hughandkettle.ui.connect
import io.github.markovcd.hughandkettle.ui.connected
import io.github.markovcd.hughandkettle.ui.connected_wmi
import io.github.markovcd.hughandkettle.ui.disconnect
import io.github.markovcd.hughandkettle.ui.match_menu
import io.github.markovcd.hughandkettle.ui.match_menu_line
import io.github.markovcd.hughandkettle.ui.mute
import io.github.markovcd.hughandkettle.ui.switcher_steps_through
import io.github.markovcd.hughandkettle.ui.theme
import io.github.markovcd.hughandkettle.wmi.LinkState
import org.jetbrains.compose.resources.stringResource

/**
 * The header's menu on the phone: the amp behind the box, which opens the list of amps; what − and + step
 * through; the loudness match, opened through [onMatch] where the amp has a volume; the mute; the link, with a
 * button that ends it or opens the link card through [onLink]; and the theme. Picking closes the menu through
 * [onClose]. Tests find the parts by `menu_amp`, `amp:<id>`, `switcher_mode:<id>`, `menu_match`, `menu_mute`,
 * `menu_connect` and `theme:<id>`.
 */
@Composable
fun ColumnScope.HeaderMenu(
    panel: PanelState,
    mode: SwitcherMode,
    onMode: (SwitcherMode) -> Unit,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
    onClose: () -> Unit,
    onLink: () -> Unit,
    onMatch: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    var amps by remember { mutableStateOf(false) }
    if (amps) {
        Heading(stringResource(Res.string.amp))
        AmpModels.all.forEach { amp ->
            DropdownMenuItem(
                text = { Text(amp.name) },
                leadingIcon = { RadioButton(selected = amp == panel.amp, onClick = null) },
                onClick = {
                    onClose()
                    panel.choose(amp)
                },
                modifier = Modifier.testTag("amp:${amp.id}"),
            )
        }
        return
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { amps = true }
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("menu_amp"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(Res.string.amp),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
            Text(panel.amp.name, style = MaterialTheme.typography.bodyLarge)
        }
        ChevronMark(colors.onSurfaceVariant)
    }
    HorizontalDivider()
    Heading(stringResource(Res.string.switcher_steps_through))
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        SwitcherMode.entries.forEachIndexed { index, choice ->
            SegmentedButton(
                selected = choice == mode,
                onClick = { onMode(choice) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = SwitcherMode.entries.size),
                icon = {},
                colors =
                    SegmentedButtonDefaults.colors(
                        activeContainerColor = colors.primary,
                        activeContentColor = colors.onPrimary,
                    ),
                modifier = Modifier.testTag("switcher_mode:${choice.id}"),
            ) { Text(stringResource(choice.label), maxLines = 1) }
        }
    }
    if (onMatch != null) {
        HorizontalDivider()
        Column(
            Modifier
                .fillMaxWidth()
                .clickable {
                    onClose()
                    onMatch()
                }.padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("menu_match"),
        ) {
            Text(stringResource(Res.string.match_menu), style = MaterialTheme.typography.bodyLarge)
            Text(
                stringResource(Res.string.match_menu_line),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
            )
        }
        HorizontalDivider()
    }
    if (panel.mutable) {
        val on = panel.muted
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(enabled = panel.linked) { panel.mute(!on) }
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .testTag("menu_mute"),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MuteMark(if (on) colors.primary else colors.onSurfaceVariant)
            Text(
                stringResource(Res.string.mute),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = on, onCheckedChange = null, enabled = panel.linked)
        }
    }
    HorizontalDivider()
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).background(if (panel.linked) colors.primary else colors.error, CircleShape))
        Column(Modifier.weight(1f)) {
            val link = panel.link
            if (link is LinkState.Connected && panel.cable == CableState.None) {
                Text(stringResource(Res.string.connected), style = MaterialTheme.typography.bodyLarge)
                Text(
                    stringResource(Res.string.connected_wmi, "${link.address}"),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurfaceVariant,
                )
            } else {
                Text(
                    LinkWords.of(link, panel.cable).text(),
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        OutlinedButton(
            onClick = {
                if (panel.linked) {
                    onClose()
                    panel.disconnect()
                } else {
                    onLink()
                }
            },
            modifier = Modifier.testTag("menu_connect"),
        ) { Text(stringResource(if (panel.linked) Res.string.disconnect else Res.string.connect)) }
    }
    HorizontalDivider()
    Heading(stringResource(Res.string.theme))
    ThemeChoice.entries.forEach { choice ->
        DropdownMenuItem(
            text = { Text(stringResource(choice.label)) },
            leadingIcon = { RadioButton(selected = choice == theme, onClick = null) },
            onClick = {
                onClose()
                onTheme(choice)
            },
            modifier = Modifier.testTag("theme:${choice.id}"),
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}
