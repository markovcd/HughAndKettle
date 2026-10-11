package io.github.markovcd.hughandkettle.amp

/** How a control's 0..127 is read: as a sweep, as off/on, or as one of a few sectors. */
public sealed interface ControlKind {
    public data object Knob : ControlKind

    public data object Switch : ControlKind

    /** The amp splits 0..127 into [options].size equal sectors, first option lowest. */
    public data class Selector(
        val options: List<String>,
    ) : ControlKind {
        init {
            require(options.size in 2..128) { "a selector needs 2..128 options" }
        }
    }
}
