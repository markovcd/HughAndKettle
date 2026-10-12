package io.github.markovcd.hughandkettle.amp

/** A tone read from a file, and a sentence for each value in the file that it had to leave out. */
public data class ToneRead(
    val tone: Tone,
    val skipped: List<String>,
)
