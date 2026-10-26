package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Writing [tone] into [preset], counted from 1. The amp stores only when its own STORE button is pressed,
 * so the write recalls the preset and keeps it as the [backup], sends the tone, waits for the amp to report
 * STORE, then sends the preset's program change, which stores, and compares the value block that answers
 * with the tone. The caller sends what each step returns, hands on every message it hears through [after],
 * and calls [gaveUp] when a step outlasts its [limit].
 */
public class PresetWriting private constructor(
    public val tone: Tone,
    public val preset: Int,
    public val stage: Stage,
    public val backup: Tone?,
    private val heard: AmpState,
) {
    public val amp: AmpModel get() = tone.amp

    /** Where the write is. */
    public sealed interface Stage {
        /** The preset is recalled, and its value block awaited as the backup. */
        public data object BackingUp : Stage

        /** The tone is sent, and the amp's STORE awaited. */
        public data object Waiting : Stage

        /** STORE was pressed and the program change sent; the stored preset's value block is awaited. */
        public data object Storing : Stage

        /** The amp stored the preset, and these values of the tone came back otherwise. */
        public data class Stored(
            val differences: List<Difference>,
        ) : Stage

        /** Nothing was written into the preset, for [why]. */
        public data class NotWritten(
            val why: Why,
        ) : Stage

        /**
         * The preset may or may not hold the tone: the program change went out but the amp never
         * confirmed, or STORE was pressed before the backup, so the recall itself may have stored what was playing.
         */
        public data object Unsure : Stage
    }

    /** Why nothing was written. */
    public enum class Why {
        /** The amp sent no value block for the preset, so it could not be backed up. */
        NoBackup,

        /** STORE was not pressed in time. */
        NoStore,

        Cancelled,

        LinkLost,
    }

    /** A value the amp stored otherwise than the tone holds, each 0..127. */
    public data class Difference(
        val control: Control,
        val stored: Int,
        val tone: Int,
    )

    /** Whether the write has ended, one way or another. */
    public val over: Boolean get() = stage is Stage.Stored || stage is Stage.NotWritten || stage == Stage.Unsure

    /** How long the current step may wait for the amp, or null once the write is [over]. */
    public val limit: Duration?
        get() =
            when (stage) {
                Stage.BackingUp, Stage.Storing -> answerLimit
                Stage.Waiting -> storeLimit
                else -> null
            }

    /** The write after the amp said [message], and what to send now. */
    public fun after(message: MidiMessage): Pair<PresetWriting, List<MidiMessage>> {
        val state = heard.after(message)
        val sysex = message as? SystemExclusive
        val block = sysex?.let(ValueBlock::parse)
        val stored = sysex?.let(ControlReport::parse)?.let { it.controller == STORE && it.value > 0 } == true
        return when (stage) {
            Stage.BackingUp -> {
                when {
                    stored -> {
                        copy(stage = Stage.Unsure) to emptyList()
                    }

                    block?.preset == preset -> {
                        val backup = Tone("Backup of ${where()}", amp, state.values)
                        copy(stage = Stage.Waiting, backup = backup, heard = AmpState(amp)) to tone.recall()
                    }

                    else -> {
                        copy(heard = state) to emptyList()
                    }
                }
            }

            Stage.Waiting -> {
                if (stored) copy(stage = Stage.Storing) to listOf(amp.recall(preset)) else this to emptyList()
            }

            Stage.Storing -> {
                if (block?.preset ==
                    preset
                ) {
                    copy(stage = Stage.Stored(differences(block))) to emptyList()
                } else {
                    this to emptyList()
                }
            }

            else -> {
                this to emptyList()
            }
        }
    }

    /** The write after its step outlasted [limit]. */
    public fun gaveUp(): PresetWriting =
        when (stage) {
            Stage.BackingUp -> copy(stage = Stage.NotWritten(Why.NoBackup))
            Stage.Waiting -> copy(stage = Stage.NotWritten(Why.NoStore))
            Stage.Storing -> copy(stage = Stage.Unsure)
            else -> this
        }

    /** The write called off by the person; once STORE has been pressed it is out of their hands. */
    public fun cancel(): PresetWriting = ended(Why.Cancelled)

    /** The write after the link to the amp dropped. */
    public fun lost(): PresetWriting = ended(Why.LinkLost)

    /** After [Stage.Unsure], a recall of the preset whose value block says whether it holds the tone. */
    public fun check(): Pair<PresetWriting, List<MidiMessage>> =
        if (stage == Stage.Unsure) copy(stage = Stage.Storing) to listOf(amp.recall(preset)) else this to emptyList()

    private fun ended(why: Why): PresetWriting =
        when (stage) {
            Stage.BackingUp, Stage.Waiting -> copy(stage = Stage.NotWritten(why))
            Stage.Storing -> copy(stage = Stage.Unsure)
            else -> this
        }

    /**
     * The tone's values [block] carries otherwise. A level the amp stored at 0 for an effect the tone has off
     * is how the amp keeps an effect switched off, so it is not one.
     */
    private fun differences(block: ValueBlock): List<Difference> {
        val stored = AmpState(amp).after(block.message()).values
        return amp.controls.mapNotNull { control ->
            val wanted = tone.values[control.id] ?: return@mapNotNull null
            if (control.controller !in block.values && control.id != "channel") return@mapNotNull null
            val got = stored[control.id] ?: return@mapNotNull null
            val off = control.needs?.let { tone.values[it] }?.let { !control.isOn(it) } == true
            when {
                control.level && off && got == 0 -> null
                same(control, got, wanted) -> null
                else -> Difference(control, got, wanted)
            }
        }
    }

    private fun same(
        control: Control,
        a: Int,
        b: Int,
    ): Boolean =
        when (val kind = control.kind) {
            ControlKind.Knob -> a == b
            ControlKind.Switch -> control.isOn(a) == control.isOn(b)
            is ControlKind.Selector -> Sectors.index(a, kind.options.size) == Sectors.index(b, kind.options.size)
        }

    private fun copy(
        stage: Stage = this.stage,
        backup: Tone? = this.backup,
        heard: AmpState = this.heard,
    ) = PresetWriting(tone, preset, stage, backup, heard)

    /** The preset as the amp shows it: bank and place in the bank where it has banks, else its number. */
    private fun where(): String = amp.slotOf(preset)?.let { "Bank ${it.bank} · ${it.slot}" } ?: "Preset $preset"

    public companion object {
        // The amp answers a recall in tens of milliseconds over the WMI-1; this leaves room for a busy network.
        public val answerLimit: Duration = 2.seconds

        // Time to put the phone down and reach the amp's panel.
        public val storeLimit: Duration = 60.seconds

        // The amp reports its STORE button on this controller: 127 pressed, 0 once stored.
        private const val STORE = 80

        /** A write of [tone] into [preset], and the recall it starts with. */
        public fun start(
            tone: Tone,
            preset: Int,
        ): Pair<PresetWriting, List<MidiMessage>> {
            val recall = tone.amp.recall(preset)
            return PresetWriting(tone, preset, Stage.BackingUp, null, AmpState(tone.amp)) to listOf(recall)
        }
    }
}
