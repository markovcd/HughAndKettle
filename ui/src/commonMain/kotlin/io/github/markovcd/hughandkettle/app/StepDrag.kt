package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The song step lifted by a drag, counted from 0, and how far it has been pulled from its place. */
internal class StepDrag {
    var step by mutableStateOf<Int?>(null)
    var lift by mutableFloatStateOf(0f)

    fun drop() {
        step = null
        lift = 0f
    }
}
