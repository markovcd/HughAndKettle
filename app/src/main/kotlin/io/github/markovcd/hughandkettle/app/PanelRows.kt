package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind

/** How a section's controls fall into rows: knobs several to a row, everything else a row of its own. */
internal object PanelRows {
    const val KNOBS_PER_ROW = 3

    fun of(
        controls: List<Control>,
        knobsPerRow: Int = KNOBS_PER_ROW,
    ): List<List<Control>> {
        val rows = ArrayList<List<Control>>()
        var knobs = ArrayList<Control>()

        fun flush() {
            if (knobs.isNotEmpty()) rows += knobs
            knobs = ArrayList()
        }
        for (control in controls) {
            if (control.kind == ControlKind.Knob) {
                knobs += control
                if (knobs.size == knobsPerRow) flush()
            } else {
                flush()
                rows += listOf(control)
            }
        }
        flush()
        return rows
    }
}
