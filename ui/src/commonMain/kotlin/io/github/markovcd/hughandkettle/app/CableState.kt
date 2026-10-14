package io.github.markovcd.hughandkettle.app

/**
 * Where the USB MIDI cable stands. A cable that fails is not retried, as a WMI-1 link is: it is
 * plugged back in and picked again.
 */
sealed interface CableState {
    data object None : CableState

    data class Opening(
        val cable: Cable,
    ) : CableState

    data class Open(
        val cable: Cable,
    ) : CableState

    /** Could not be opened, or was unplugged while open. */
    data class Failed(
        val cable: Cable,
        val reason: String,
    ) : CableState

    /** True while the cable is open or being opened. */
    val active: Boolean get() = this is Opening || this is Open
}
