package io.github.markovcd.hughandkettle.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * The third signal of docs/design/colors.md, recording: a red [mark] and its [onMark] ink, on a [ground] edged
 * by [edge]. It stands apart from amber (live) and rose (wrong) in lightness as well as hue.
 */
public data class RecordingColors(
    val mark: Color,
    val onMark: Color,
    val ground: Color,
    val edge: Color,
) {
    public companion object {
        public val Dark: RecordingColors =
            RecordingColors(Color(0xFFFF6B5B), Color(0xFF1A0A08), Color(0xFF2A1613), Color(0xFF5A2D26))

        public val Light: RecordingColors =
            RecordingColors(Color(0xFFB3261E), Color(0xFFFFFFFF), Color(0xFFFFE4DF), Color(0xFFE8A59C))

        /** The recording colors that go with the scheme in use. */
        @Composable
        public fun current(): RecordingColors = if (MaterialTheme.colorScheme === Palette.Dark) Dark else Light
    }
}
