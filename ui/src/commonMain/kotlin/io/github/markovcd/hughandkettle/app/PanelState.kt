package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.AmpState
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.RecallAnswers
import io.github.markovcd.hughandkettle.amp.TapTempo
import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.amp.VolumeTrim
import io.github.markovcd.hughandkettle.midi.ControlChange
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.midi.ProgramChange
import io.github.markovcd.hughandkettle.wmi.LinkState
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiDefaults
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlin.time.Duration

/**
 * What the panel shows and does: the link's state or the cable's, which amp is behind the box, and the
 * amp's state, as sent and heard. The amp picked for a box is remembered through [rememberAmp] and taken up again
 * from [ampChoices] whenever the panel turns to that box.
 */
class PanelState(
    amp: AmpModel,
    private val holder: LinkHolder,
    private val scope: CoroutineScope,
    lastWmi: Flow<WmiAddress?>,
    ampChoices: Flow<Map<WmiAddress, AmpModel>> = flowOf(emptyMap()),
    private val rememberAmp: suspend (WmiAddress, AmpModel) -> Unit = { _, _ -> },
) {
    var amp by mutableStateOf(amp)
        private set
    var host by mutableStateOf(WmiDefaults.ACCESS_POINT_HOST)
    var port by mutableStateOf(WmiDefaults.MIDI_PORT.toString())
    var link by mutableStateOf(holder.state.value)
        private set
    var cable by mutableStateOf(holder.cable.value)
        private set

    /** The USB MIDI cables plugged into the phone, offered while there is no link. */
    var cables by mutableStateOf(holder.cables.value)
        private set
    var failure by mutableStateOf<PanelFailure?>(null)
        private set
    var state by mutableStateOf(AmpState(amp))
        private set

    /** The ids of controls the amp last moved to a value the panel had not set, until the panel sets them. */
    var moved by mutableStateOf(emptySet<String>())
        private set

    /** The tempo lock and the volume trim, answering every preset recalled and channel switched to. */
    private var answers by mutableStateOf(RecallAnswers(trim = trimFor(amp, 0)))

    /** The delay time held across preset recalls, while locked. */
    val lock: TempoLock? get() = answers.lock

    /** How many recalled presets the lock has given the held time back, so the panel can show each one. */
    var restored by mutableStateOf(0)
        private set

    /** The amount added to every recalled preset's and channel's volume, standing in for Master; null for an amp with no volume. */
    val trim: VolumeTrim? get() = answers.trim

    /** How many recalled presets and channels the trim has moved the volume of, so the panel can show each one. */
    var trimmed by mutableStateOf(0)
        private set

    /** The last box a link reached, offered first. */
    var last by mutableStateOf<WmiAddress?>(null)
        private set

    /** True from linking to a box with no amp picked for it, until one is picked or the question is put away. */
    var asking by mutableStateOf(false)
        private set

    /** Whether a box heard is linked to without a tap; off from a disconnect until the next connect. */
    private var linksByItself = true

    private var choices: Map<WmiAddress, AmpModel> = emptyMap()
    private var choicesRead = false
    private var pickedByHand = false
    private var askedAbout: WmiAddress? = null
    private var taps = TapTempo()

    init {
        scope.launch {
            holder.state.collect {
                link = it
                follow()
            }
        }
        scope.launch {
            ampChoices.collect {
                choices = it
                choicesRead = true
                follow()
            }
        }
        scope.launch {
            holder.cable.collect {
                cable = it
                follow()
            }
        }
        scope.launch { holder.cables.collect { cables = it } }
        scope.launch { holder.heard.collect(::heard) }
        scope.launch {
            lastWmi.collect {
                last = it
                if (it != null && (host.isBlank() || host == WmiDefaults.ACCESS_POINT_HOST)) {
                    host = it.host
                    port = it.port.toString()
                }
                follow()
            }
        }
    }

    /**
     * The box the panel is about: none over a cable, the one linked or being linked to, else the last one reached,
     * else the one the fields name.
     */
    val box: WmiAddress?
        get() =
            when (val now = link) {
                LinkState.Idle -> if (cable.active) null else last ?: WmiAddress.parse("${host.trim()}:${port.trim()}")
                is LinkState.Connecting -> now.address
                is LinkState.Connected -> now.address
                is LinkState.Retrying -> now.address
            }

    /** Puts [amp] on the panel, and remembers it for the box when there is one. */
    fun choose(amp: AmpModel) {
        show(amp)
        pickedByHand = true
        asking = false
        val box = box ?: return
        scope.launch { rememberAmp(box, amp) }
    }

    /**
     * Shows the amp picked for the box. A box linked to with none picked keeps the amp on the panel, and asks
     * which amp it is unless one was picked by hand.
     */
    private fun follow() {
        val box = box ?: return
        val chosen = choices[box]
        when {
            chosen != null -> {
                show(chosen)
            }

            choicesRead && link != LinkState.Idle -> {
                if (box != askedAbout && !pickedByHand) asking = true
                askedAbout = box
                scope.launch { rememberAmp(box, amp) }
            }
        }
    }

    /** Keeps the amp on the panel for the box without asking again. */
    fun settle() {
        asking = false
    }

    /** A different amp starts from knowing nothing about it. */
    private fun show(amp: AmpModel) {
        if (amp == this.amp) return
        this.amp = amp
        state = AmpState(amp)
        moved = emptySet()
        taps = TapTempo()
        answers = RecallAnswers(trim = trimFor(amp, trim?.trim ?: 0))
    }

    /** The fields are editable only while there is nothing to protect. */
    val idle: Boolean get() = link == LinkState.Idle && !cable.active

    /** True while messages reach the amp, over a WMI-1 or a cable. */
    val linked: Boolean get() = link is LinkState.Connected || cable is CableState.Open

    /** True while something is wrong that the person should look at. */
    val warning: Boolean
        get() = failure != null || link is LinkState.Retrying || cable is CableState.Failed

    /**
     * The boxes heard on the network. While idle, the panel links by itself to the last box used when it is
     * among them, else to the only one heard.
     */
    fun offered(boxes: List<WmiAddress>) {
        if (!idle || !linksByItself) return
        connect(last?.takeIf { it in boxes } ?: boxes.singleOrNull() ?: return)
    }

    fun connect() {
        val address = WmiAddress.parse("${host.trim()}:${port.trim()}")
        if (address == null) {
            failure = PanelFailure.NeedsAddress
            return
        }
        connect(address)
    }

    /** Connects to [address], showing it in the fields. */
    fun connect(address: WmiAddress) {
        host = address.host
        port = address.port.toString()
        failure = null
        linksByItself = true
        holder.connect(address)
    }

    /** Opens [cable] in place of any WMI-1. */
    fun connect(cable: Cable) {
        failure = null
        holder.connect(cable)
    }

    fun disconnect() {
        linksByItself = false
        failure = null
        holder.stop()
    }

    fun set(
        control: Control,
        value: Int,
    ) = send(control.set(value))

    /**
     * A tap on [control]'s beat at [at], from any fixed point; from the second tap on, the knob moves to
     * the beat, as near as its scale reaches.
     */
    fun tap(
        control: Control,
        at: Duration,
    ) {
        val scale = control.scale ?: return
        taps = taps.after(at)
        val beat = taps.beat ?: return
        set(control, scale.value(beat.inWholeMilliseconds.toInt()))
    }

    /** Holds [control]'s time, at its value now, across preset recalls; or lets it go. */
    fun lock(
        control: Control,
        on: Boolean,
    ) {
        answers = answers.copy(lock = if (on) TempoLock(control, state.values[control.id]) else null)
    }

    /** Adds [amount] to the volume of every preset recalled and channel switched to from now on, and to the channel on now. */
    fun trim(amount: Int) {
        val (next, change) = trim?.retrimmed(amount) ?: return
        answers = answers.copy(trim = next)
        change?.let { transmit(listOf(it)) }
    }

    /**
     * Recalls [preset], counted from 1 as the amp shows it. Under a trim the recall goes behind the amp's mute,
     * unless it is muted already, so the preset's own volume never sounds.
     */
    fun recall(preset: Int) = recall(amp.recall(preset), emptyList())

    /** Sends a step of a song: its preset recalled as [recall] recalls one, then the rest of [messages]. */
    fun cue(messages: List<MidiMessage>) {
        val preset = messages.filterIsInstance<ProgramChange>().firstOrNull() ?: return send(messages)
        recall(preset, messages - preset)
    }

    private fun recall(
        recall: ProgramChange,
        then: List<MidiMessage>,
    ) {
        val mute = amp.control("mute")
        val muted = mute != null && state.values[mute.id]?.let(mute::isOn) == true
        val (next, messages) = answers.recall(recall, mute, muted, then)
        answers = next
        // One transmit, so the recall and what follows it reach the amp in order.
        transmit(messages)
        if (!next.muting) return
        scope.launch {
            delay(VolumeTrim.muteLimit)
            val (unmuted, lift) = answers.unmuted()
            answers = unmuted
            lift?.let { transmit(listOf(it)) }
        }
    }

    /** Sends every value [tone] holds, in order, so a channel switch lands before the knobs it carries. */
    fun recall(tone: Tone) {
        send(answers.tone(tone.recall()))
    }

    /** The tone [name] holding every value the amp is known to be at, its volume without the trim. */
    fun tone(name: String): Tone = Tone(name, state.amp, trim?.untrimmed(state.values) ?: state.values)

    private fun send(message: MidiMessage) = send(listOf(message))

    /** Sends what the person asked for: a volume among it is answered with itself if a value block comes first. */
    private fun send(messages: List<MidiMessage>) {
        answers = answers.sent(messages)
        transmit(messages)
    }

    private fun transmit(messages: List<MidiMessage>) {
        state = messages.fold(state) { before, message -> before.after(message) }
        moved =
            if (messages.any { it is ProgramChange }) {
                emptySet()
            } else {
                moved - messages.filterIsInstance<ControlChange>().mapNotNull { amp.controlFor(it) }.toSet()
            }
        if (!linked) return
        scope.launch(Dispatchers.IO) {
            for (message in messages) {
                holder.send(message).onFailure {
                    failure = PanelFailure.SendFailed(it.message ?: "link lost")
                    return@launch
                }
            }
        }
    }

    /**
     * Takes in what the amp said; a preset it recalled while locked gets the held time back, and under a trim
     * its volume trimmed.
     */
    private fun heard(message: MidiMessage) {
        val before = state
        state = before.after(message)
        moved =
            if (message is ProgramChange) {
                emptySet()
            } else {
                moved + state.values.filter { (id, value) -> before.values[id] != value }.keys
            }
        val answered = answers.heard(message)
        answers = answered.answers
        if (answered.trimmed.isNotEmpty()) trimmed++
        if (answered.restored != null) restored++
        if (answered.replies.isNotEmpty()) transmit(answered.replies)
    }

    private fun trimFor(
        amp: AmpModel,
        amount: Int,
    ): VolumeTrim? = amp.control("volume")?.let { VolumeTrim.of(it, amount) }

    private fun AmpModel.controlFor(change: ControlChange): String? =
        controls
            .firstOrNull {
                it.controller ==
                    change.controller
            }?.id
}
