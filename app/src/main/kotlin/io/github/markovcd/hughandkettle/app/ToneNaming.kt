package io.github.markovcd.hughandkettle.app

import io.github.markovcd.hughandkettle.amp.Tone

/** What the tone name dialog is naming: a new tone from the panel, or one already on the shelf. */
internal sealed interface ToneNaming {
    data object New : ToneNaming

    data class Existing(
        val tone: Tone,
    ) : ToneNaming
}
