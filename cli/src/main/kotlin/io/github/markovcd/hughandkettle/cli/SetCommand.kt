package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.PrintWriter

/** Sets one control of an amp by name, or recalls a preset, and prints what went out. */
public object SetCommand : Command {
    override val name: String = "set"
    override val usage: String =
        """
        hk set <host[:port]> <amp-id> <control-id> <0..127 | on | off | option>
        hk set <host[:port]> <amp-id> preset <1..128>
        """.trimIndent()

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val target = args.positional.getOrNull(0)?.let(WmiAddress::parse)
        if (target == null || args.positional.size != 4) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp = AmpArgument.parse(args.positional[1]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val (message, fields) =
            setting(amp, args.positional[2], args.positional[3]).orExit(err, ExitCodes.BAD_ARGUMENTS) { return it }
        val link =
            WmiLink.open(target.host, target.port).getOrElse {
                err.println("cannot reach $target: ${it.message}")
                return ExitCodes.NO_LINK
            }
        link.use {
            it.send(message).getOrElse { failure ->
                err.println("send failed: ${failure.message}")
                return ExitCodes.NO_LINK
            }
        }
        out.println(
            Json.line(
                "host" to target.host,
                "port" to target.port,
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
