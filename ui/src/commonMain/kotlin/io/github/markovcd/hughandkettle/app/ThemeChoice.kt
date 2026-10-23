package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.theme_dark
import io.github.markovcd.hughandkettle.ui.theme_light
import io.github.markovcd.hughandkettle.ui.theme_system
import org.jetbrains.compose.resources.StringResource

/**
 * Which colors the app wears: dark unless the person picks otherwise. Written to the phone as
 * `<version> <id>`; the ids are persisted and never renamed. A value that does not read is dark.
 */
enum class ThemeChoice(
    val id: String,
    val label: StringResource,
) {
    Dark("dark", Res.string.theme_dark),
    Light("light", Res.string.theme_light),
    System("system", Res.string.theme_system),
    ;

    fun write(): String = "$VERSION $id"

    /** Whether this choice wears the dark colors, given whether the system is [systemDark]. */
    fun dark(systemDark: Boolean): Boolean = this == Dark || (this == System && systemDark)

    companion object {
        const val VERSION: Int = 1

        fun read(text: String?): ThemeChoice {
            if (text == null || text.substringBefore(' ').toIntOrNull() != VERSION) return Dark
            return entries.firstOrNull { it.id == text.substringAfter(' ') } ?: Dark
        }
    }
}
