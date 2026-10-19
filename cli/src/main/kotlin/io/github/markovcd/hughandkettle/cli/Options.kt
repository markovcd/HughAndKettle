package io.github.markovcd.hughandkettle.cli

/** Parsed command-line arguments: `--name value` and `--flag` options, and the positionals in order. */
public class Options private constructor(
    public val positional: List<String>,
    private val named: Map<String, String>,
) {
    public fun value(name: String): String? = named[name]

    public fun flag(name: String): Boolean = named[name] == FLAG

    /** An integer option or [default]; a value that is not a whole number is reported, not thrown. */
    public fun int(
        name: String,
        default: Int,
    ): Result<Int> {
        val raw = named[name] ?: return Result.success(default)
        return raw.toIntOrNull()?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("--$name needs a whole number, got '$raw'"))
    }

    /** An integer option, or null when it is not given; a value that is not a whole number is reported, not thrown. */
    public fun optionalInt(name: String): Result<Int?> = if (named[name] == null) Result.success(null) else int(name, 0)

    public companion object {
        private const val FLAG = ""

        /** Options that take a value; every other `--name` is a flag. */
        public fun parse(
            args: List<String>,
            valued: Set<String>,
        ): Options {
            val positional = ArrayList<String>()
            val named = LinkedHashMap<String, String>()
            var i = 0
            while (i < args.size) {
                val arg = args[i]
                when {
                    arg.startsWith("--") && arg.substring(2) in valued && i + 1 < args.size -> {
                        named[arg.substring(2)] = args[i + 1]
                        i++
                    }

                    arg.startsWith("--") -> {
                        named[arg.substring(2)] = FLAG
                    }

                    else -> {
                        positional += arg
                    }
                }
                i++
            }
            return Options(positional, named)
        }
    }
}
