package io.github.markovcd.hughandkettle.cli

import java.io.PrintWriter

/** The `hk` command line: `hk <command> ...`. */
public object Cli {
    private val commands: List<Command> =
        listOf(
            SendCommand,
            SetCommand,
            StateCommand,
            ToneCommand,
            PresetCommand,
            ListenCommand,
            LockCommand,
            CableCommand,
            DiscoverCommand,
            FakeWmiCommand,
            LatencyCommand,
        )
    private val valuedOptions =
        setOf("channel", "seconds", "port", "name", "broadcast-to", "keep-open", "dir", "count", "names")

    public fun run(
        args: List<String>,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val command = commands.firstOrNull { it.name == args.firstOrNull() }
        if (command == null) {
            err.println("usage:")
            commands.forEach { err.println(it.usage.prependIndent("  ")) }
            return ExitCodes.BAD_ARGUMENTS
        }
        return command.run(Options.parse(args.drop(1), valuedOptions), out, err)
    }
}
