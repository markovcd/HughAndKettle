package io.github.markovcd.hughandkettle.app.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** The app's own colors and type; dynamic color is off so a knob looks the same on every phone. */
@Composable
public fun HughAndKettleTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (dark) Palette.Dark else Palette.Light,
        typography = hughAndKettleTypography(),
        content = content,
    )
}
