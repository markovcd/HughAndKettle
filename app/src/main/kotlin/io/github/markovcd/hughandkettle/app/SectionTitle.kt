package io.github.markovcd.hughandkettle.app

import androidx.annotation.StringRes
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Section

/** The words over a section, kept out of the amp tables; the sections a tab or a bar speaks for have none. */
@StringRes
internal fun Section.title(): Int? =
    when (this) {
        Section.Preamp -> R.string.section_preamp
        Section.PowerAmp -> R.string.section_power_amp
        Section.Header, Section.Switching, Section.Fx -> null
    }
