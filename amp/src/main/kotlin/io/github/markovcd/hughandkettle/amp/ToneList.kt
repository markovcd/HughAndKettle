package io.github.markovcd.hughandkettle.amp

/** The tones in a folder in their order, and a sentence for each file that could not be read. */
public data class ToneList(
    val tones: List<Tone>,
    val problems: List<String>,
)
