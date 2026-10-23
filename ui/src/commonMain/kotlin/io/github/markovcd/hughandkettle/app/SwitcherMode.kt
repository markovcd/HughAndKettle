package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.switcher_presets
import io.github.markovcd.hughandkettle.ui.switcher_tones
import org.jetbrains.compose.resources.StringResource

/**
 * What the header's − and + step through: the amp's presets unless the person picks the pinned tones.
 * Written to the phone as `<version> <id>`; the ids are persisted and never renamed. A value that does
 * not read is presets.
 */
enum class SwitcherMode(
    val id: String,
    val label: StringResource,
) {
    Presets("presets", Res.string.switcher_presets),
    Tones("tones", Res.string.switcher_tones),
    ;

    fun write(): String = "$VERSION $id"

    companion object {
        const val VERSION: Int = 1

        fun read(text: String?): SwitcherMode {
            if (text == null || text.substringBefore(' ').toIntOrNull() != VERSION) return Presets
            return entries.firstOrNull { it.id == text.substringAfter(' ') } ?: Presets
        }
    }
}
