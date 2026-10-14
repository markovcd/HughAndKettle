package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind

/** How controls fall into rows: knobs up to four to a row, everything else, a time knob too, a row of its own. */
internal object PanelRows {
    const val KNOBS_PER_ROW = 4

    fun of(controls: List<Control>): List<List<Control>> {
        val rows = ArrayList<List<Control>>()
        var knobs = ArrayList<Control>()

        fun flush() {
            if (knobs.isNotEmpty()) rows += knobs
            knobs = ArrayList()
        }
        for (control in controls) {
            if (control.kind == ControlKind.Knob && !control.timed) {
                knobs += control
                if (knobs.size == KNOBS_PER_ROW) flush()
            } else {
                flush()
                rows += listOf(control)
            }
        }
        flush()
        return rows
    }
}
