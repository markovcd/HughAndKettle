package io.github.markovcd.hughandkettle.wmi

/** Something that moves a [LinkState]: a request from the user, or news from the network or the clock. */
public sealed interface LinkEvent {
    public data class Connect(
        val address: WmiAddress,
    ) : LinkEvent

    public data object Stop : LinkEvent

    public data object Opened : LinkEvent

    public data class Failed(
        val failure: LinkFailure,
    ) : LinkEvent

    /** The clock moved; a retry that is due starts. */
    public data object Tick : LinkEvent
}
