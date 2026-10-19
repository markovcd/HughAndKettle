package io.github.markovcd.hughandkettle.amp

/**
 * The songs kept for one amp model in their order, the name of the [open] one, and the name of the song
 * [carried] over from the single song an older Hugh & Kettle kept, when this read carried it.
 */
public data class SongList(
    val songs: List<KeptSong>,
    val open: String? = null,
    val carried: String? = null,
) {
    public fun find(name: String): KeptSong? = songs.firstOrNull { it.name.equals(name, ignoreCase = true) }

    /** [name], or the first of "[name] 2", "[name] 3" and on that no song here has. */
    public fun apart(name: String): String =
        generateSequence(1) { it + 1 }
            .map { if (it == 1) name else "$name $it" }
            .first { find(it) == null }
}
