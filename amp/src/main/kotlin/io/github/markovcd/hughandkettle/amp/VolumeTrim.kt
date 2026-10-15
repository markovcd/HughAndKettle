package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.midi.SystemExclusive
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * A [trim] added to the volume of every preset recalled and every channel switched to, standing in for
 * Master, which is not on MIDI. Each preset and channel keeps its own volume and the trim moves them all
 * together, so the balance between them holds.
 *
 * A channel switched back to still carries the volume the trim gave it, so the trim remembers, per
 * channel, the volume last heard and the untrimmed one it stands for. A preset's stored volume is
 * remembered from its value block, so the next recall of it from the footswitch is answered on the
 * program change, ahead of the block. A recall from the app goes behind the amp's mute, lifted with the
 * answer to the block.
 */
@ConsistentCopyVisibility
public data class VolumeTrim private constructor(
    val control: Control,
    val trim: Int,
    /** The mute to lift with the next value block's answer, while a recall is behind it. */
    val muting: Control? = null,
    private val channel: Int? = null,
    private val heard: Map<Int, Heard> = emptyMap(),
    private val stored: Map<Int, Int> = emptyMap(),
    private val early: Int? = null,
    private val sent: Int? = null,
) {
    init {
        require(trim in -127..127) { "a trim must be -127..127, got $trim" }
    }

    /** A channel's volume as last heard, and the untrimmed volume it stands for. */
    private data class Heard(
        val volume: Int,
        val untrimmed: Int,
    )

    /** The trim after [message] heard from the amp. */
    public fun after(message: MidiMessage): VolumeTrim =
        when (message) {
            is ProgramChange -> {
                copy(early = early(message))
            }

            is SystemExclusive -> {
                val block = ValueBlock.parse(message)
                val report = ControlReport.parse(message)
                when {
                    block != null -> {
                        after(block)
                    }

                    report != null && report.controller == control.controller -> {
                        reported(
                            control.fromReport(report.value),
                        )
                    }

                    else -> {
                        this
                    }
                }
            }

            else -> {
                this
            }
        }

    /** The trim after the app sent [message]: a volume it sets is answered with itself if a block comes first. */
    public fun sent(message: MidiMessage): VolumeTrim =
        if (message is ControlChange && message.controller == control.controller) copy(sent = message.value) else this

    /** What to send on hearing [message]: the trimmed volume, and the mute lifted after a block. */
    public fun restore(message: MidiMessage): List<ControlChange> =
        when (message) {
            is ProgramChange -> {
                listOfNotNull(early(message)?.let(control::set))
            }

            is SystemExclusive -> {
                val block = ValueBlock.parse(message) ?: return emptyList()
                val carried = carried(block)
                val volume = carried?.let { target(block, it) }?.takeIf { it != carried }?.let(control::set)
                listOfNotNull(volume, muting?.switched(false))
            }

            else -> {
                emptyList()
            }
        }

    /** The trim at [trim], and the change that moves the channel on now to it. */
    public fun retrimmed(trim: Int): Pair<VolumeTrim, ControlChange?> {
        val on = channel ?: return copy(trim = trim) to null
        val now = heard[on] ?: return copy(trim = trim) to null
        val volume = trimmed(now.untrimmed, trim)
        val next = copy(trim = trim, heard = heard + (on to Heard(volume, now.untrimmed)))
        return next to control.set(volume).takeIf { volume != now.volume }
    }

    /** The messages that recall [preset] behind [mute], and the trim waiting to lift it. */
    public fun recall(
        preset: ProgramChange,
        mute: Control,
    ): Pair<VolumeTrim, List<MidiMessage>> = copy(muting = mute) to listOf(mute.switched(true), preset)

    /** The trim with no recall behind the mute any more, and the change that lifts it, for a block that never came. */
    public fun unmuted(): Pair<VolumeTrim, ControlChange?> = copy(muting = null) to muting?.switched(false)

    /** [messages] with every volume trimmed, so a tone recalled plays at the trim. */
    public fun trimmed(messages: List<MidiMessage>): List<MidiMessage> =
        messages.map {
            if (it is ControlChange &&
                it.controller == control.controller
            ) {
                it.copy(value = trimmed(it.value, trim))
            } else {
                it
            }
        }

    /** [values] of controls by id with the trim taken off the volume, so a tone saved keeps the amp's own. */
    public fun untrimmed(values: Map<String, Int>): Map<String, Int> =
        values.mapValues { (id, value) -> if (id == control.id) trimmed(value, -trim) else value }

    private fun after(block: ValueBlock): VolumeTrim {
        val carried = carried(block) ?: return copy(channel = block.channel, muting = null)
        val untrimmed = untrimmed(block, carried)
        val now = sent?.let { Heard(it, trimmed(it, -trim)) } ?: Heard(trimmed(untrimmed, trim), untrimmed)
        return copy(
            channel = block.channel,
            heard = heard + (block.channel to now),
            stored = if (block.preset != null) stored + (block.preset to untrimmed) else stored,
            early = null,
            sent = null,
            muting = null,
        )
    }

    /** The trim after the amp reported its volume at [volume], set by hand, by the app or by the trim. */
    private fun reported(volume: Int): VolumeTrim {
        val now = channel ?: return copy(sent = null)
        val known = heard[now]?.takeIf { it.volume == volume }
        return copy(heard = heard + (now to (known ?: Heard(volume, trimmed(volume, -trim)))), sent = null)
    }

    private fun carried(block: ValueBlock): Int? = block.values[control.controller]?.let(control::fromReport)

    /**
     * The untrimmed volume [block] stands for: a recalled preset's own; for a channel switched to, the one its
     * volume was trimmed from, if the trim set it.
     */
    private fun untrimmed(
        block: ValueBlock,
        carried: Int,
    ): Int =
        when {
            block.preset != null && early != null && carried == early -> stored[block.preset] ?: carried
            block.preset != null -> carried
            else -> heard[block.channel]?.takeIf { it.volume == carried }?.untrimmed ?: carried
        }

    /** The volume to answer [block] with: one the app just sent, else the untrimmed one trimmed. */
    private fun target(
        block: ValueBlock,
        carried: Int,
    ): Int = sent ?: trimmed(untrimmed(block, carried), trim)

    /** The trimmed volume of a preset [recall] calls up, known from its last block, while there is a trim. */
    private fun early(recall: ProgramChange): Int? =
        stored[recall.program + 1]?.takeIf { trim != 0 }?.let { trimmed(it, trim) }

    private fun trimmed(
        volume: Int,
        trim: Int,
    ): Int = (volume + trim).coerceIn(0, 127)

    public companion object {
        /** How long a recall stays behind the mute waiting for its value block before the mute is lifted anyway. */
        public val muteLimit: Duration = 1.seconds

        /** A trim of [trim] on [control], the amp's volume, with nothing heard yet. */
        public fun of(
            control: Control,
            trim: Int = 0,
        ): VolumeTrim = VolumeTrim(control, trim)
    }
}
