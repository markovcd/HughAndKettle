package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind

/**
 * How a section's controls fall into rows: knobs several to a row, everything else a row of its own. A
 * group of knobs that would not fit in what is left of a row starts the next one.
 */
internal object PanelRows {
    const val KNOBS_PER_ROW = 3

    fun of(controls: List<Control>): List<List<Control>> {
        val rows = ArrayList<List<Control>>()
        var knobs = ArrayList<Control>()

        fun flush() {
            if (knobs.isNotEmpty()) rows += knobs
            knobs = ArrayList()
        }
        for ((at, control) in controls.withIndex()) {
            if (control.kind == ControlKind.Knob) {
                if (control.startsGroup && knobs.size + groupSize(controls, at) > KNOBS_PER_ROW) flush()
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

    /** The knobs from [start] up to the next group, switch or selector. */
    private fun groupSize(
        controls: List<Control>,
        start: Int,
    ): Int {
        val rest = controls.drop(start)
        val size = rest.withIndex().indexOfFirst { (i, c) -> c.kind != ControlKind.Knob || (i > 0 && c.startsGroup) }
        return if (size < 0) rest.size else size
    }
}
