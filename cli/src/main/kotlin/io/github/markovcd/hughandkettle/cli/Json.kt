package io.github.markovcd.hughandkettle.cli

/** One JSON object per line is all the CLI prints; this is the one place that knows how to spell it. */
public object Json {
    public fun line(vararg fields: Pair<String, Any?>): String =
        fields.joinToString(",", "{", "}") { (key, value) -> "${quote(key)}:${render(value)}" }

    private fun render(value: Any?): String =
        when (value) {
            null -> "null"
            is Boolean, is Int, is Long -> value.toString()
            is List<*> -> value.joinToString(",", "[", "]") { render(it) }
            else -> quote(value.toString())
        }

    private fun quote(text: String): String =
        buildString {
            append('"')
            for (c in text) {
                when {
                    c == '"' -> append("\\\"")
                    c == '\\' -> append("\\\\")
                    c < ' ' -> append("\\u%04x".format(c.code))
                    else -> append(c)
                }
            }
            append('"')
        }
}
