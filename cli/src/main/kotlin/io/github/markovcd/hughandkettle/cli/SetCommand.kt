package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.midi.MidiMessage
import java.io.PrintWriter

/** Sets one control of an amp by name, or recalls a preset, and prints what went out. */
public object SetCommand : Command {
    override val name: String = "set"
    override val usage: String =
        """
        hk set ${AmpTarget.USAGE} <amp-id> <control-id> <0..127 | on | off | option>
        hk set ${AmpTarget.USAGE} <amp-id> preset <1..128>
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(AmpTarget::parse)
        if (target == null || args.positional.size != 4) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val (message, fields) =
            setting(amp, args.positional[2], args.positional[3]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val link = QueuedLink.open(target).orExit(err, ExitCodes.NO_LINK) { return it }
        link.use { it.sendAll(listOf(message)) }.orExit(err, ExitCodes.NO_LINK) { return it }
        out.println(
            Json.line(
                *link.where.toTypedArray(),
                "amp" to amp.id,
                *fields.toTypedArray(),
                *MidiJson.fields(message).toTypedArray(),
            ),
        )
        return ExitCodes.OK
    }

    private fun setting(
        amp: AmpModel,
        what: String,
        value: String,
    ): Result<Pair<MidiMessage, List<Pair<String, Any?>>>> {
        if (what == "preset") {
            val preset =
                value.toIntOrNull()?.takeIf { it in 1..amp.presetCount }
                    ?: return Result.failure(
                        IllegalArgumentException("${amp.id} has presets 1..${amp.presetCount}, got '$value'"),
                    )
            val slot = amp.slotOf(preset)
            val fields =
                listOf("preset" to preset) + (slot?.let { listOf("bank" to it.bank, "slot" to it.slot) } ?: emptyList())
            return Result.success(amp.recall(preset) to fields)
        }
        val control =
            amp.control(what)
                ?: return Result.failure(
                    IllegalArgumentException(
                        "${amp.id} has no control '$what'; it has preset, ${amp.controls.joinToString(", ") { it.id }}",
                    ),
                )
        return ControlValue.parse(control, value).map { it to listOf("control" to control.id) }
    }
}
