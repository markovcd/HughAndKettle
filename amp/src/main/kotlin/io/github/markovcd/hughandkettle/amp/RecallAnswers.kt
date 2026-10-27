package io.github.markovcd.hughandkettle.amp

import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange

/**
 * The tempo [lock] and the volume [trim] together, either of them off: what the app sends back after the amp
 * recalls a preset or switches a channel, and how it sends a recall of its own. The panel and `hk` both go
 * through one of these, so a recall is answered the same way whoever drives the amp.
 */
public data class RecallAnswers(
    val lock: TempoLock? = null,
    val trim: VolumeTrim? = null,
) {
    /** The answers after a message heard, and what they send back. */
    public data class Heard(
        val answers: RecallAnswers,
        /** The trimmed volume, and the mute lifted, after a recall. */
        val trimmed: List<ControlChange>,
        /** The locked time put back after a recall. */
        val restored: ControlChange?,
    ) {
        val replies: List<ControlChange> get() = trimmed + listOfNotNull(restored)
    }

    /** True while a recall is behind the mute, waiting for its value block. */
    val muting: Boolean get() = trim?.muting != null

    /** The answers after hearing [message] from the amp, and what to send back. */
    public fun heard(message: MidiMessage): Heard =
        Heard(
            RecallAnswers(lock?.after(message), trim?.after(message)),
            trim?.restore(message).orEmpty(),
            lock?.restore(message),
        )

    /** The answers after sending [messages] someone asked for. */
    public fun sent(messages: List<MidiMessage>): RecallAnswers =
        messages.fold(this) { answers, message ->
            RecallAnswers(answers.lock?.after(message), answers.trim?.sent(message))
        }

    /**
     * The messages that recall [preset] and then send [then]. Under a trim or a correction the recall goes behind [mute],
     * unless the amp is [muted] already, so the preset's own volume never sounds.
     */
    public fun recall(
        preset: ProgramChange,
        mute: Control?,
        muted: Boolean,
        then: List<MidiMessage> = emptyList(),
    ): Pair<RecallAnswers, List<MidiMessage>> {
        val held = trim
        if (held == null || !held.moves(preset) || mute == null || muted) {
            val messages = listOf(preset) + then
            return sent(messages) to messages
        }
        val (next, behind) = held.recall(preset, mute)
        val messages = behind + then
        return copy(trim = next).sent(messages) to messages
    }

    /** The answers with no recall behind the mute, and the change that lifts it, for a value block that never came. */
    public fun unmuted(): Pair<RecallAnswers, ControlChange?> {
        val held = trim?.takeIf { it.muting != null } ?: return this to null
        val (next, lift) = held.unmuted()
        return copy(trim = next) to lift
    }

    /** A tone's [messages] with its delay time kept by the lock and its volume trimmed. */
    public fun tone(messages: List<MidiMessage>): List<MidiMessage> {
        val kept = lock?.kept(messages) ?: messages
        return trim?.trimmed(kept) ?: kept
    }
}
