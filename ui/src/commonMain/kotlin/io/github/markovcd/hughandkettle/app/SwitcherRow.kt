package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.header_menu
import io.github.markovcd.hughandkettle.ui.preset
import io.github.markovcd.hughandkettle.ui.preset_bank_slot
import io.github.markovcd.hughandkettle.ui.status_muted
import io.github.markovcd.hughandkettle.ui.status_offline
import io.github.markovcd.hughandkettle.ui.switcher_next
import io.github.markovcd.hughandkettle.ui.switcher_no_pinned
import io.github.markovcd.hughandkettle.ui.switcher_none_loaded
import io.github.markovcd.hughandkettle.ui.switcher_previous
import io.github.markovcd.hughandkettle.ui.switcher_tone
import io.github.markovcd.hughandkettle.ui.switcher_tone_of
import io.github.markovcd.hughandkettle.ui.switcher_tone_pinned
import org.jetbrains.compose.resources.stringResource

/**
 * The header on every tab: − and + around what they step through, the amp's presets or the pinned tones as
 * [mode] says and on while [enabled], a chip while offline, and the [menu] behind its dots, where the desktop has
 * none. While muted, a mute mark takes the linked dot's place on the dots' corner. A tap on the middle opens the preset or tone picker; with no tone pinned it goes to the Tones tab
 * through [onTones]. While a song plays, − and + are off. Tests find the row by `switcher`, its parts by
 * `switcher_prev`, `switcher_next`, `switcher_pick`, `switcher_caption` and `switcher_line`, the mute mark by
 * `status_muted`, the chip by `status_offline`, the menu by `header_menu` and the link dialog by `link_dialog`.
 */
@Composable
fun SwitcherRow(
    panel: PanelState,
    discovery: DiscoveryState,
    presetNames: PresetNamesState,
    shelf: ToneShelf,
    songs: SongState,
    mode: SwitcherMode,
    enabled: Boolean,
    onTones: () -> Unit,
    menu: (@Composable ColumnScope.(close: () -> Unit, link: () -> Unit) -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val linked = panel.linked
    val pinned = shelf.pinned
    val steps = enabled && !songs.busy && (mode == SwitcherMode.Presets || pinned.isNotEmpty())
    var picking by remember { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var linking by rememberSaveable { mutableStateOf(false) }
    val step = { by: Int -> if (mode == SwitcherMode.Presets) panel.step(by) else shelf.step(by) }
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 8.dp)
            .testTag("switcher"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton("−", stringResource(Res.string.switcher_previous), steps, { step(-1) }, "switcher_prev")
        val noTones = mode == SwitcherMode.Tones && pinned.isEmpty() && shelf.loaded == null
        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(enabled = noTones || enabled) { if (noTones) onTones() else picking = true }
                .padding(horizontal = 4.dp)
                .testTag("switcher_pick"),
            verticalArrangement = Arrangement.Center,
        ) {
            when (mode) {
                SwitcherMode.Presets -> PresetMiddle(panel, presetNames)
                SwitcherMode.Tones -> ToneMiddle(shelf)
            }
        }
        StepButton("+", stringResource(Res.string.switcher_next), steps, { step(1) }, "switcher_next")
        Offline(panel, onOpen = { if (menu != null) menuOpen = true })
        val mute = panel.amp.control("mute")
        val muted = linked && mute != null && mute.isOn(panel.state.values[mute.id] ?: 0)
        // With no menu to carry it, as on the desktop, the mark stands at the end of the row.
        if (menu == null && muted) MutedMark()
        if (menu != null) {
            Box {
                val words = stringResource(Res.string.header_menu)
                IconButton(
                    onClick = { menuOpen = true },
                    modifier = Modifier.size(44.dp).testTag("header_menu").semantics { contentDescription = words },
                ) { DotsMark(MaterialTheme.colorScheme.onSurfaceVariant) }
                if (muted) {
                    MutedMark(Modifier.align(Alignment.TopEnd).padding(top = 4.dp, end = 4.dp))
                } else if (linked) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp)
                            .size(8.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                            .testTag("status_linked"),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    modifier = Modifier.width(300.dp),
                ) {
                    menu({ menuOpen = false }, {
                        menuOpen = false
                        linking = true
                    })
                }
            }
        }
    }
    if (picking) {
        when (mode) {
            SwitcherMode.Presets -> {
                PresetPicker(panel, presetNames, onClose = { picking = false })
            }

            SwitcherMode.Tones -> {
                TonePicker(shelf, panel.amp, onClose = { picking = false }, onManage = {
                    picking = false
                    onTones()
                })
            }
        }
    }
    if (linking) {
        Dialog(onDismissRequest = { linking = false }) {
            LinkCard(panel, discovery, Modifier.padding(8.dp).testTag("link_dialog"))
        }
    }
}

@Composable
private fun StepButton(
    sign: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        contentPadding = PaddingValues(0.dp),
        modifier = Modifier.size(44.dp).semantics { contentDescription = description }.testTag(tag),
    ) { Text(sign, style = MaterialTheme.typography.titleLarge) }
}

/** The preset's number in bold, then its name, else its bank and slot, under a "Preset" caption. */
@Composable
private fun PresetMiddle(
    panel: PanelState,
    names: PresetNamesState,
) {
    val preset = panel.state.preset
    val name = preset?.let { names.names[it] }
    val slot = preset?.let(panel.amp::slotOf)
    Caption(stringResource(Res.string.preset))
    Row(Modifier.testTag("switcher_line"), verticalAlignment = Alignment.CenterVertically) {
        Text(preset?.toString() ?: "–", style = MaterialTheme.typography.headlineSmall, fontSize = 24.sp, maxLines = 1)
        val under = name ?: slot?.let { stringResource(Res.string.preset_bank_slot, it.bank, it.slot) }
        under?.let {
            Text(
                "  $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The tone loaded, with its place among the pinned ones when it is one of them; how many are pinned while
 * none is loaded; or that none is pinned.
 */
@Composable
private fun ToneMiddle(shelf: ToneShelf) {
    val pinned = shelf.pinned
    val loaded = shelf.loaded
    val colors = MaterialTheme.colorScheme
    val at = loaded?.let(pinned::indexOf) ?: -1
    Caption(
        when {
            at >= 0 -> stringResource(Res.string.switcher_tone_of, at + 1, pinned.size)
            loaded == null && pinned.isNotEmpty() -> stringResource(Res.string.switcher_tone_pinned, pinned.size)
            else -> stringResource(Res.string.switcher_tone)
        },
    )
    Row(
        Modifier.testTag("switcher_line"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            loaded != null -> {
                TabMark(PanelTab.Tones, colors.primary, 18.dp)
                Text(
                    loaded.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            pinned.isNotEmpty() -> {
                Text(
                    stringResource(Res.string.switcher_none_loaded),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    fontStyle = FontStyle.Italic,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                )
            }

            else -> {
                Text(
                    stringResource(Res.string.switcher_no_pinned),
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 16.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun Caption(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        modifier = Modifier.testTag("switcher_caption"),
    )
}

/** A red chip while offline, a tap away from the menu. */
@Composable
private fun Offline(
    panel: PanelState,
    onOpen: () -> Unit,
) {
    if (panel.linked) return
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onOpen,
        shape = CircleShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.error),
        modifier = Modifier.testTag("status_offline"),
    ) {
        Text(
            stringResource(Res.string.status_offline),
            color = colors.error,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

/** The amp muted, at a glance: a small amber disc with the struck speaker in the dark ink. */
@Composable
private fun MutedMark(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val words = stringResource(Res.string.status_muted)
    Box(
        modifier
            .size(14.dp)
            .background(colors.primary, CircleShape)
            .semantics { contentDescription = words }
            .testTag("status_muted"),
        contentAlignment = Alignment.Center,
    ) { MuteMark(colors.onPrimary, 10.dp) }
}
