package io.github.markovcd.hughandkettle.app

/** A USB MIDI cable plugged into the phone: Android's id for it while it stays plugged in, and its name. */
data class Cable(
    val id: Int,
    val name: String,
)
