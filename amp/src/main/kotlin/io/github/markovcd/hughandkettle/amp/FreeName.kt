package io.github.markovcd.hughandkettle.amp

/** How a name is set apart from the names already kept: "Clean", then "Clean 2", "Clean 3" and on. */
internal object FreeName {
    /** [name], or the first of "[name] 2", "[name] 3" and on that is not [taken]. */
    fun of(
        name: String,
        taken: (String) -> Boolean,
    ): String =
        generateSequence(1) { it + 1 }
            .map { if (it == 1) name else "$name $it" }
            .first { !taken(it) }
}
