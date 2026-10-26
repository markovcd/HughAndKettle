package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.NoteLength

/** A `--note` option, a quarter when not given; a failure lists the notes there are. */
public object NoteArgument {
    public fun parse(args: Options): Result<NoteLength> {
        val text = args.value("note") ?: return Result.success(NoteLength.QUARTER)
        return NoteLength.of(text)?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("no note '$text'; there are ${NoteLength.IDS}"))
    }
}
