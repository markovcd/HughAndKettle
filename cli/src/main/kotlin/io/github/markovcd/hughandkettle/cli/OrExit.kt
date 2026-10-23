package io.github.markovcd.hughandkettle.cli

import java.io.PrintWriter

/**
 * The value, or else the failure's message on [err] and [exit] with [code]. A command passes
 * `{ return it }`, so it ends the run with that code.
 */
internal inline fun <T> Result<T>.orExit(
    err: PrintWriter,
    code: Int,
    exit: (Int) -> Nothing,
): T =
    getOrElse {
        err.println(it.message)
        exit(code)
    }
