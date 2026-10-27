package io.github.markovcd.hughandkettle.amp

/** A microphone the loudness match listens through, opened for one soundcheck at a time. */
public fun interface Microphone {
    /** Starts listening; fails when the microphone is missing, taken or not allowed. */
    public fun open(): Result<MicrophoneInput>
}
