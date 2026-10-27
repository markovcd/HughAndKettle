package io.github.markovcd.hughandkettle.desktop

import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.app.PanelState
import io.github.markovcd.hughandkettle.app.SongState
import io.github.markovcd.hughandkettle.app.ToneShelf

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

    /** Whether the amp is muted, by its Mute switch. */
    val muted: Boolean get() = panel.muted

    fun mute(on: Boolean) = panel.mute(on)

    /** Whether the song is counting in or playing. */
    val playing: Boolean get() = songs.busy

    fun playOrStop() = if (playing) songs.stop() else songs.start()

    /** Whether a take is being recorded. */
    val recording: Boolean get() = songs.recording

    /** Records a take starting on [from], else on the preset the amp is on; or stops the one being recorded. */
    fun recordOrStop(from: Tone? = null) = if (recording) songs.stopRecording() else songs.record(from)
}
