package io.github.markovcd.hughandkettle.app.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/** The two schemes from docs/design/colors.md. Dark is the one designed for the stage; light is derived. */
internal object Palette {
    val Dark: ColorScheme =
        darkColorScheme(
            primary = Color(0xFFFFB547),
            onPrimary = Color(0xFF422C00),
            primaryContainer = Color(0xFF5E4100),
            onPrimaryContainer = Color(0xFFFFDDAF),
            secondary = Color(0xFFD8C3A8),
            onSecondary = Color(0xFF3B2E1C),
            secondaryContainer = Color(0xFF534430),
            onSecondaryContainer = Color(0xFFF5DFC3),
            tertiary = Color(0xFFA7C9D6),
            onTertiary = Color(0xFF0E3440),
            tertiaryContainer = Color(0xFF284B58),
            onTertiaryContainer = Color(0xFFC6E7F3),
            error = Color(0xFFFF8FAE),
            onError = Color(0xFF5C0024),
            errorContainer = Color(0xFF861B3F),
            onErrorContainer = Color(0xFFFFD9E1),
            background = Color(0xFF14110F),
            onBackground = Color(0xFFECE4DA),
            surface = Color(0xFF14110F),
            onSurface = Color(0xFFECE4DA),
            surfaceVariant = Color(0xFF3D3731),
            onSurfaceVariant = Color(0xFFC4B9AC),
            outline = Color(0xFF8E8377),
            outlineVariant = Color(0xFF4A433C),
            surfaceContainerLowest = Color(0xFF0F0D0B),
            surfaceContainerLow = Color(0xFF1C1916),
            surfaceContainer = Color(0xFF211D1A),
            surfaceContainerHigh = Color(0xFF2B2723),
            surfaceContainerHighest = Color(0xFF36312C),
        )

    val Light: ColorScheme =
        lightColorScheme(
            primary = Color(0xFF7A5200),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFFFDDAF),
            onPrimaryContainer = Color(0xFF281800),
            secondary = Color(0xFF6B5B45),
            onSecondary = Color(0xFFFFFFFF),
            secondaryContainer = Color(0xFFF5DFC3),
            onSecondaryContainer = Color(0xFF251A0B),
            tertiary = Color(0xFF39616F),
            onTertiary = Color(0xFFFFFFFF),
            tertiaryContainer = Color(0xFFC6E7F3),
            onTertiaryContainer = Color(0xFF001F28),
            error = Color(0xFFA8274F),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFFFD9E1),
            onErrorContainer = Color(0xFF3F0016),
            background = Color(0xFFFBF7F2),
            onBackground = Color(0xFF1F1B17),
            surface = Color(0xFFFBF7F2),
            onSurface = Color(0xFF1F1B17),
            surfaceVariant = Color(0xFFEDE3D6),
            onSurfaceVariant = Color(0xFF4E463D),
            outline = Color(0xFF7F7468),
            outlineVariant = Color(0xFFD2C6B8),
            surfaceContainerLowest = Color(0xFFFFFFFF),
            surfaceContainerLow = Color(0xFFF6F0E9),
            surfaceContainer = Color(0xFFF0E9E1),
            surfaceContainerHigh = Color(0xFFEAE3DA),
            surfaceContainerHighest = Color(0xFFE4DCD2),
        )
}
