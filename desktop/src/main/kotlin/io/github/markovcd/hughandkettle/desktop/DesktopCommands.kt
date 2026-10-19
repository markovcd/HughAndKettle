package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.amp.Control
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

    /** A tap on the beat at [at], from any fixed point, for the amp's time knob. */
    fun tap(at: Duration) {
        panel.amp.controls
            .firstOrNull(Control::timed)
            ?.let { panel.tap(it, at) }
    }

    /** Whether the song is counting in or playing. */
    val playing: Boolean get() = songs.countdown != null || songs.position != null

    fun playOrStop() = if (playing) songs.stop() else songs.start()
}
