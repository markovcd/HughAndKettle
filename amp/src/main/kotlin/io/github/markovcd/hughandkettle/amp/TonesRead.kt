package io.github.markovcd.hughandkettle.amp

/** Tones read from a file, and a sentence for each thing in it that had to be left out. */
public data class TonesRead(
    val tones: List<Tone>,
    val skipped: List<String>,
) {
    /**
     * These tones, each whose name is in [taken] or comes earlier in the list renamed with the next free
     * number after it: a second "Clean" becomes "Clean 2". Names are compared regardless of case, as
     * [ToneStore] keeps them.
     */
    public fun namedApartFrom(taken: Collection<String>): TonesRead {
        val used = taken.mapTo(HashSet()) { it.lowercase() }
        val named =
            tones.map { tone ->
                val name =
                    generateSequence(1) { it + 1 }
                        .map { if (it == 1) tone.name else "${tone.name} $it" }
                        .first { it.lowercase() !in used }
                used += name.lowercase()
                tone.copy(name = name)
            }
        return copy(tones = named)
    }
}
