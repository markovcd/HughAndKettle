package io.github.markovcd.hughandkettle.cli

/** Each exit code means one thing, so a script can branch on it. */
public object ExitCodes {
    public const val OK: Int = 0
    public const val BAD_ARGUMENTS: Int = 2
    public const val NO_LINK: Int = 3
    public const val NOTHING_FOUND: Int = 4
}
