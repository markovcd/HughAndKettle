package io.github.markovcd.hughandkettle.amp

import kotlin.math.roundToInt

/**
 * A note length the delay repeats at while the tempo lock holds it: [beats] of a quarter-note beat, so a
 * dotted eighth is 0.75. The [id] is what `hk` takes and tests find a note by: `1/8`, `1/8d` dotted,
 * `1/8t` triplet. Ids are written into song files: never renamed.
 */
public data class NoteLength(
    val id: String,
    val beats: Double,
) {
    /** How long the note lasts at [tempo] beats per minute, to the nearest millisecond. */
    public fun ms(tempo: Int): Int = (MINUTE_MS * beats / tempo).roundToInt()

    /** The tempo at which the note lasts [ms], to the nearest beat per minute. */
    public fun tempo(ms: Int): Int = (MINUTE_MS * beats / ms).roundToInt()

    public companion object {
        public val QUARTER: NoteLength = NoteLength("1/4", 1.0)

        /** Every note the delay takes, in the order the panel lays them out. */
        public val ALL: List<NoteLength> =
            listOf(
                NoteLength("1/2", 2.0),
                QUARTER,
                NoteLength("1/8", 0.5),
                NoteLength("1/16", 0.25),
                NoteLength("1/4d", 1.5),
                NoteLength("1/8d", 0.75),
                NoteLength("1/4t", 2.0 / 3),
                NoteLength("1/8t", 1.0 / 3),
            )

        /** Every note's id, as a list in a message. */
        public val IDS: String = ALL.joinToString(", ") { it.id }

        /** The note [id] names, or null for none of them. */
        public fun of(id: String): NoteLength? = ALL.firstOrNull { it.id == id }

        private const val MINUTE_MS = 60_000.0
    }
}
