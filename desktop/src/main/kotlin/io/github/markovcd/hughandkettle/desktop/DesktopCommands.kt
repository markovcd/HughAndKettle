package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.app.PanelState
import io.github.markovcd.hughandkettle.app.SongState
import io.github.markovcd.hughandkettle.app.ToneShelf
import kotlin.time.Duration

/** What the menu bar's items and their shortcuts do, so each can be done and checked without a window. */
class DesktopCommands(
    private val panel: PanelState,
    private val shelf: ToneShelf,
    private val songs: SongState,
) {
    fun nextTone() {
        shelf.step(1)
    }

    fun previousTone() {
        shelf.step(-1)
    }

    /** A tap on the beat at [at], from any fixed point, for the song's tempo. */
    fun tap(at: Duration) = songs.tap(at)

    /** Whether the amp is muted, by its Mute switch. */
    val muted: Boolean get() = panel.muted

    fun mute(on: Boolean) = panel.mute(on)

    /** Whether the song is counting in or playing. */
    val playing: Boolean get() = songs.busy

    fun playOrStop() = if (playing) songs.stop() else songs.start()
}
