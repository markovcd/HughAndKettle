package io.github.markovcd.hughandkettle.amp

/** A binary property list's reference to another object of a keyed archive, by its place in `$objects`. */
@JvmInline
internal value class PlistUid(
    val index: Int,
)
