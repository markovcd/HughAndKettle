package io.github.markovcd.hughandkettle.amp

/** A song kept on the phone under its [name]: the [song], or why its file cannot be read in [problem]. */
public data class KeptSong(
    val name: String,
    val song: Song?,
    val problem: String?,
)
