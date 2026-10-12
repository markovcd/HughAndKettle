package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/** Marks a control whose effect is switched off; tests match on it. */
internal val EffectOff = SemanticsPropertyKey<Boolean>("EffectOff")

private var SemanticsPropertyReceiver.effectOff by EffectOff

/** Dims a control whose effect is switched off, still turnable, and tells a screen reader [words]. */
internal fun Modifier.effectOff(
    silent: Boolean,
    words: String,
): Modifier =
    if (!silent) {
        this
    } else {
        alpha(SILENT).semantics {
            effectOff = true
            stateDescription = words
        }
    }

private const val SILENT = 0.45f
