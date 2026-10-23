package io.github.markovcd.hughandkettle.amp

/** What came of importing a song file. */
public sealed interface SongImport {
    /** Kept under [name], set apart from the name the file [asked] for when another song had it. */
    public data class Kept(
        val name: String,
        val asked: String,
    ) : SongImport

    /** Refused whole: the file is for the amp model with the id [amp]. */
    public data class OtherAmp(
        val amp: String,
    ) : SongImport
}
