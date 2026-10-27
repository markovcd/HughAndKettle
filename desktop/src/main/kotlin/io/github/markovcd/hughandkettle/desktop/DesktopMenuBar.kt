package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.SwitcherMode
import io.github.markovcd.hughandkettle.app.ThemeChoice
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.amp
import io.github.markovcd.hughandkettle.ui.disconnect
import io.github.markovcd.hughandkettle.ui.link
import io.github.markovcd.hughandkettle.ui.mute
import io.github.markovcd.hughandkettle.ui.song
import io.github.markovcd.hughandkettle.ui.song_play
import io.github.markovcd.hughandkettle.ui.song_record
import io.github.markovcd.hughandkettle.ui.song_record_stop
import io.github.markovcd.hughandkettle.ui.song_stop
import io.github.markovcd.hughandkettle.ui.switcher_steps_through
import io.github.markovcd.hughandkettle.ui.theme
import io.github.markovcd.hughandkettle.ui.theme_system_desktop
import io.github.markovcd.hughandkettle.ui.tone_next
import io.github.markovcd.hughandkettle.ui.tone_previous
import io.github.markovcd.hughandkettle.ui.tones
import org.jetbrains.compose.resources.stringResource

/**
 * The window's menus, in place of the phone's header menu: the link, the amp behind the box and its mute, the
 * tones with their shortcuts and what the header's − and + step through, the song and its takes, and the theme.
 */
@Composable
fun FrameWindowScope.DesktopMenuBar(
    screen: DesktopScreen,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
    mode: SwitcherMode,
    onMode: (SwitcherMode) -> Unit,
) {
    val panel = screen.panel
    val commands = screen.commands
    MenuBar {
        Menu(stringResource(Res.string.link)) {
            Item(stringResource(Res.string.disconnect), enabled = !panel.idle, onClick = panel::disconnect)
        }
        Menu(stringResource(Res.string.amp)) {
            AmpModels.all.forEach { amp ->
                RadioButtonItem(amp.name, selected = amp == panel.amp, onClick = { panel.choose(amp) })
            }
            Separator()
            CheckboxItem(
                stringResource(Res.string.mute),
                checked = commands.muted,
                enabled = panel.linked,
                onCheckedChange = commands::mute,
            )
        }
        Menu(stringResource(Res.string.tones)) {
            Item(
                stringResource(Res.string.tone_previous),
                shortcut = DesktopOs.shortcut(Key.DirectionUp),
                enabled = panel.linked,
                onClick = commands::previousTone,
            )
            Item(
                stringResource(Res.string.tone_next),
                shortcut = DesktopOs.shortcut(Key.DirectionDown),
                enabled = panel.linked,
                onClick = commands::nextTone,
            )
            Separator()
            SwitcherMode.entries.forEach { choice ->
                RadioButtonItem(
                    "${stringResource(Res.string.switcher_steps_through)}: ${stringResource(choice.label)}",
                    selected = choice == mode,
                    onClick = { onMode(choice) },
                )
            }
        }
        Menu(stringResource(Res.string.song)) {
            Item(
                stringResource(if (commands.playing) Res.string.song_stop else Res.string.song_play),
                shortcut = DesktopOs.shortcut(Key.P),
                enabled = panel.linked,
                onClick = commands::playOrStop,
            )
            Item(
                stringResource(if (commands.recording) Res.string.song_record_stop else Res.string.song_record),
                shortcut = DesktopOs.shortcut(Key.R),
                enabled = panel.linked && !commands.playing,
                onClick = { commands.recordOrStop(screen.shelf.loaded.takeIf { mode == SwitcherMode.Tones }) },
            )
        }
        Menu(stringResource(Res.string.theme)) {
            ThemeChoice.entries.forEach { choice ->
                val label = if (choice == ThemeChoice.System) Res.string.theme_system_desktop else choice.label
                RadioButtonItem(stringResource(label), selected = choice == theme, onClick = { onTheme(choice) })
            }
        }
    }
}
