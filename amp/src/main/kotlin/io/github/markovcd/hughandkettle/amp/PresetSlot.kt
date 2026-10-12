package io.github.markovcd.hughandkettle.amp

/** A preset as an amp with banks shows it: bank and slot, both counted from 1. */
public data class PresetSlot(
    val bank: Int,
    val slot: Int,
)
