package io.github.markovcd.hughandkettle.app

/**
 * What the song list has to tell after a change: the older single song carried over, an imported song set
 * apart from one of its name, a song for another amp refused, or a change that failed.
 */
sealed interface SongNotice {
    data class Carried(
        val name: String,
    ) : SongNotice

    data class ImportedAs(
        val name: String,
        val kept: String,
    ) : SongNotice

    data class OtherAmp(
        val amp: String,
    ) : SongNotice

    data class Failed(
        val reason: String,
    ) : SongNotice
}
