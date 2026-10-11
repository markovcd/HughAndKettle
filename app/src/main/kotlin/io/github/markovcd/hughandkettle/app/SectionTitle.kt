package io.github.markovcd.hughandkettle.app

import androidx.annotation.StringRes
import io.github.markovcd.hughandkettle.R
import io.github.markovcd.hughandkettle.amp.Section

/** The words for a section, kept out of the amp tables. */
@StringRes
internal fun Section.title(): Int =
    when (this) {
        Section.Switching -> R.string.section_switching
        Section.Amp -> R.string.section_amp
        Section.Fx -> R.string.section_fx
        Section.Extras -> R.string.section_extras
    }
