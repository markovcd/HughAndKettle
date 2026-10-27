package io.github.markovcd.hughandkettle.amp

/** An open [Microphone], read one frame of [LevelFrame.LENGTH] at a time. */
public interface MicrophoneInput : AutoCloseable {
    /** Waits for the next frame; fails when the microphone stops. */
    public fun read(): Result<LevelFrame>
}
