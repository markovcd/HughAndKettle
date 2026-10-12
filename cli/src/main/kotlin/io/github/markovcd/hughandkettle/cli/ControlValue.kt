package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.midi.ControlChange

/** How a value is typed for a control on the command line: 0..127, `on`/`off`, or an option's name. */
public object ControlValue {
    /** The message that sets [control] to [text]; a failure says what the control takes. */
    public fun parse(
        control: Control,
        text: String,
    ): Result<ControlChange> {
        val number = text.toIntOrNull()?.takeIf { it in 0..127 }
        val message =
            when (val kind = control.kind) {
                ControlKind.Knob -> {
                    number?.let(control::set)
                }

                ControlKind.Switch -> {
                    when (text.lowercase()) {
                        "on" -> control.switched(true)
                        "off" -> control.switched(false)
                        else -> number?.let(control::set)
                    }
                }

                is ControlKind.Selector -> {
                    kind.options
                        .indexOfFirst { it.equals(text, ignoreCase = true) }
                        .takeIf { it >= 0 }
                        ?.let(control::selected)
                }
            }
        return message?.let { Result.success(it) }
            ?: Result.failure(IllegalArgumentException("'${control.id}' takes ${takes(control)}, got '$text'"))
    }

    private fun takes(control: Control): String =
        when (val kind = control.kind) {
            ControlKind.Knob -> "0..127"
            ControlKind.Switch -> "on, off or 0..127"
            is ControlKind.Selector -> kind.options.joinToString(", ") { "'$it'" }
        }
}
