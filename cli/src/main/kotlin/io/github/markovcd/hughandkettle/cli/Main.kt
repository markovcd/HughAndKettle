package io.github.markovcd.hughandkettle.cli

import java.io.PrintWriter
import kotlin.system.exitProcess

public fun main(args: Array<String>) {
    val out = PrintWriter(System.out, true)
    val err = PrintWriter(System.err, true)
    val code = Cli.run(args.toList(), out, err)
    out.flush()
    err.flush()
    exitProcess(code)
}
