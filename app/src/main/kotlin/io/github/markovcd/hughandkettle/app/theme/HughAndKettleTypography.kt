package io.github.markovcd.hughandkettle.app.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.R

/** IBM Plex Sans Condensed for everything, Plex Mono for addresses and ports (docs/design/type.md). */
internal val PlexSansCondensed: FontFamily =
    FontFamily(
        Font(R.font.ibm_plex_sans_condensed_regular, FontWeight.Normal),
        Font(R.font.ibm_plex_sans_condensed_medium, FontWeight.Medium),
        Font(R.font.ibm_plex_sans_condensed_semibold, FontWeight.SemiBold),
    )

internal val PlexMono: FontFamily = FontFamily(Font(R.font.ibm_plex_mono_regular, FontWeight.Normal))

/** Tabular figures, so a value's box never moves when 99 becomes 100. */
private const val TABULAR = "tnum"

internal val HughAndKettleTypography: Typography =
    Typography(
        displaySmall =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 40.sp,
                lineHeight = 48.sp,
                fontFeatureSettings = TABULAR,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                lineHeight = 36.sp,
                fontFeatureSettings = TABULAR,
            ),
        titleLarge =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.Medium,
                fontSize = 22.sp,
                lineHeight = 28.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.Medium,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.6.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = PlexSansCondensed,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                lineHeight = 20.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = PlexMono,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
            ),
    )
