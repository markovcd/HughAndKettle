package io.github.markovcd.hughandkettle.app

import androidx.annotation.StringRes
import io.github.markovcd.hughandkettle.R

/**
 * Which colors the app wears: dark unless the person picks otherwise. Written to the phone as
 * `<version> <id>`; the ids are persisted and never renamed. A value that does not read is dark.
 */
enum class ThemeChoice(
    val id: String,
    @param:StringRes val label: Int,
) {
    Dark("dark", R.string.theme_dark),
    Light("light", R.string.theme_light),
    System("system", R.string.theme_system),
    ;

    fun write(): String = "$VERSION $id"

    companion object {
        const val VERSION: Int = 1

        fun read(text: String?): ThemeChoice {
            if (text == null || text.substringBefore(' ').toIntOrNull() != VERSION) return Dark
            return entries.firstOrNull { it.id == text.substringAfter(' ') } ?: Dark
        }
    }
}
