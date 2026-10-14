package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Section
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.section_power_amp
import io.github.markovcd.hughandkettle.ui.section_preamp
import org.jetbrains.compose.resources.StringResource

/** The words over a section, kept out of the amp tables; the sections a tab or a bar speaks for have none. */
internal fun Section.title(): StringResource? =
    when (this) {
        Section.Preamp -> Res.string.section_preamp
        Section.PowerAmp -> Res.string.section_power_amp
        Section.Header, Section.Switching, Section.Fx -> null
    }
