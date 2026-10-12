package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/** Marks a control the amp moved to a value the panel had not set; tests match on it. */
internal val MovedOnAmp = SemanticsPropertyKey<Boolean>("MovedOnAmp")

private var SemanticsPropertyReceiver.movedOnAmp by MovedOnAmp

/** Outlines a control the amp moved in [color], and tells a screen reader [words]. */
internal fun Modifier.movedMark(
    moved: Boolean,
    color: Color,
    words: String,
): Modifier =
    if (!moved) {
        this
    } else {
        border(2.dp, color, RoundedCornerShape(12.dp)).semantics {
            movedOnAmp = true
            stateDescription = words
        }
    }
