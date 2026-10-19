package io.github.markovcd.hughandkettle.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyShortcut
import androidx.compose.ui.window.FrameWindowScope
import androidx.compose.ui.window.MenuBar
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.app.ThemeChoice
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.amp
import io.github.markovcd.hughandkettle.ui.disconnect
import io.github.markovcd.hughandkettle.ui.link
import io.github.markovcd.hughandkettle.ui.song
import io.github.markovcd.hughandkettle.ui.song_play
import io.github.markovcd.hughandkettle.ui.song_stop
import io.github.markovcd.hughandkettle.ui.tap
import io.github.markovcd.hughandkettle.ui.theme
import io.github.markovcd.hughandkettle.ui.theme_system_desktop
import io.github.markovcd.hughandkettle.ui.tone_next
import io.github.markovcd.hughandkettle.ui.tone_previous
import io.github.markovcd.hughandkettle.ui.tones
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The window's menus, in place of the phone's settings menu: the link, the amp behind the box, the tones and
 * the tempo with their shortcuts, the song, and the theme.
 */
@Composable
fun FrameWindowScope.DesktopMenuBar(
    screen: DesktopScreen,
    theme: ThemeChoice,
    onTheme: (ThemeChoice) -> Unit,
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
            Item(
                stringResource(Res.string.tap),
                shortcut = KeyShortcut(Key.T, ctrl = true),
                enabled = panel.linked,
                onClick = { commands.tap(System.nanoTime().nanoseconds) },
            )
        }
        Menu(stringResource(Res.string.tones)) {
            Item(
                stringResource(Res.string.tone_previous),
                shortcut = KeyShortcut(Key.DirectionUp, ctrl = true),
                enabled = panel.linked,
                onClick = commands::previousTone,
            )
            Item(
                stringResource(Res.string.tone_next),
                shortcut = KeyShortcut(Key.DirectionDown, ctrl = true),
                enabled = panel.linked,
                onClick = commands::nextTone,
            )
        }
        Menu(stringResource(Res.string.song)) {
            Item(
                stringResource(if (commands.playing) Res.string.song_stop else Res.string.song_play),
                shortcut = KeyShortcut(Key.P, ctrl = true),
                enabled = panel.linked,
                onClick = commands::playOrStop,
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
