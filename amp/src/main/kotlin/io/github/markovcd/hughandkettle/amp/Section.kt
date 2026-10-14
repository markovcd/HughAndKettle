package io.github.markovcd.hughandkettle.amp

/**
 * Where a control sits on the panel: the [Header] beside the amp's name, the [Switching] always in
 * view under the preset, then the amp's [Preamp] and [PowerAmp] and its effects, [Fx].
 */
public enum class Section {
    Header,
    Switching,
    Preamp,
    PowerAmp,
    Fx,
}
