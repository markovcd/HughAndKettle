package io.github.markovcd.hughandkettle.cli

import java.io.PrintWriter

/** A command is a function of its arguments and two writers; stdout carries only the answer. */
public interface Command {
    public val name: String
    public val usage: String

    public fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int
}
