package io.github.markovcd.hughandkettle.amp

/** Whether a note at a tempo is a delay time the amp reaches; past either end the time holds at that end. */
public enum class NoteReach {
    Fits,
    TooShort,
    TooLong,
}
