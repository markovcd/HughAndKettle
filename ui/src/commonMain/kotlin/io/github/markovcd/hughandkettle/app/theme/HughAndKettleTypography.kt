package io.github.markovcd.hughandkettle.app.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.ibm_plex_mono_regular
import io.github.markovcd.hughandkettle.ui.ibm_plex_sans_condensed_medium
import io.github.markovcd.hughandkettle.ui.ibm_plex_sans_condensed_regular
import io.github.markovcd.hughandkettle.ui.ibm_plex_sans_condensed_semibold
import org.jetbrains.compose.resources.Font

/**
 * IBM Plex Sans Condensed for everything, Plex Mono for addresses and ports (docs/design/type.md).
 * Composable because a font is a resource the platform loads.
 */
@Composable
internal fun hughAndKettleTypography(): Typography {
    val plexSansCondensed =
        FontFamily(
            Font(Res.font.ibm_plex_sans_condensed_regular, FontWeight.Normal),
            Font(Res.font.ibm_plex_sans_condensed_medium, FontWeight.Medium),
            Font(Res.font.ibm_plex_sans_condensed_semibold, FontWeight.SemiBold),
        )
    val plexMono = FontFamily(Font(Res.font.ibm_plex_mono_regular, FontWeight.Normal))
    return typography(plexSansCondensed, plexMono)
}

/** Tabular figures, so a value's box never moves when 99 becomes 100. */
private const val TABULAR = "tnum"

private fun typography(
    plexSansCondensed: FontFamily,
    plexMono: FontFamily,
): Typography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontFeatureSettings = TABULAR,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontFeatureSettings = TABULAR,
            ),
        titleLarge =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.6.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = plexSansCondensed,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = plexMono,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
    )
