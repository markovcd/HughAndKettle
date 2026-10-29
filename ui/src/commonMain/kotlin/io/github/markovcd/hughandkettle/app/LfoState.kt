package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.ControlKind
import io.github.markovcd.hughandkettle.amp.Lfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random
import kotlin.time.Duration

/**
 * The LFOs on the effect cards, at most one a card, each sweeping a control behind the card's switch. While
 * any runs, every [pace] one of them, in turn, sends where it is by [now], but only while its effect is on
 * and no tone is being written. They carry on across recalls and song steps, and all stop when the link
 * goes. Switching one off puts its control back where it was; a tempo lock taken on its time stops it where
 * it is.
 */
class LfoState internal constructor(
    private val panel: PanelState,
    private val scope: CoroutineScope,
    private val now: () -> Duration,
    private val pace: suspend () -> Unit,
) {
    /** The LFOs running, by the id of the effect switch whose card holds each. */
    var running by mutableStateOf(emptyMap<String, Lfo>())
        private set

    /** The last LFO set on each card, running or not, so switching one on again starts where it was left. */
    private var kept = emptyMap<String, Lfo>()

    /** When each running LFO started, on [now]'s clock. */
    private var started = emptyMap<String, Duration>()

    /** The value each running LFO's control had before it started, to put back. */
    private var before = emptyMap<String, Int?>()

    private var ticking: Job? = null

    /** The card whose LFO sent last, so the next tick starts with the one after it. */
    private var lastSent: String? = null

    /** The controls an LFO on [switch]'s card can sweep: the knobs and selectors behind it. */
    fun targets(switch: Control): List<Control> =
        panel.amp.controls.filter { it.needs == switch.id && it.kind != ControlKind.Switch }

    /** The LFO set on [switch]'s card, running or last left there, else one on its first level around its value. */
    fun of(switch: Control): Lfo? {
        kept[switch.id]?.let { return it }
        val targets = targets(switch)
        val target = targets.firstOrNull(Control::level) ?: targets.firstOrNull() ?: return null
        return Lfo.around(target, panel.state.values[target.id])
    }

    /** Starts or stops the LFO on [switch]'s card. */
    fun switch(
        switch: Control,
        on: Boolean,
    ) {
        if (!on) return stop(switch.id, putBack = true)
        if (switch.id in running) return
        run(switch.id, of(switch) ?: return)
    }

    /** Sets the LFO on [switch]'s card to [lfo], running it from where it is if it runs. */
    fun set(
        switch: Control,
        lfo: Lfo,
    ) {
        val was = running[switch.id]
        kept = kept + (switch.id to lfo)
        if (was == null) return
        if (was.control.id != lfo.control.id) {
            putBack(was.control, before[switch.id])
            before = before + (switch.id to panel.state.values[lfo.control.id])
            freeTime(lfo.control)
        }
        running = running + (switch.id to lfo)
    }

    /** Sweeps [target] on [switch]'s card in place of what it swept, around where [target] is. */
    fun aim(
        switch: Control,
        target: Control,
    ) {
        val lfo = of(switch) ?: return
        if (lfo.control.id == target.id) return
        set(switch, Lfo.around(target, panel.state.values[target.id]).copy(shape = lfo.shape, hz = lfo.hz))
    }

    /**
     * Gives every LFO running on an effect that is on a shape, a rate and ends from [random], on the control it
     * moves; the LFOs as they were.
     */
    internal fun roll(random: Random): Map<String, Lfo> {
        val was = running
        was.forEach { (switch, lfo) ->
            val effect = panel.amp.control(switch) ?: return@forEach
            if (effect.isOn(panel.state.values[switch] ?: 0)) set(effect, lfo.rolled(random))
        }
        return was
    }

    /** Puts back [lfos], as [roll] gave them, on the cards still running one on the same control. */
    internal fun unroll(lfos: Map<String, Lfo>) {
        lfos.forEach { (switch, lfo) ->
            val effect = panel.amp.control(switch) ?: return@forEach
            if (running[switch]?.control?.id == lfo.control.id) set(effect, lfo)
        }
    }

    /** Stops any LFO on [control] where it is, for a tempo lock that now holds it. */
    internal fun yieldTo(control: Control) {
        running.filterValues { it.control.controller == control.controller }.keys.forEach { stop(it, putBack = false) }
    }

    /** Stops every LFO where it is, for a link that is gone. */
    internal fun dropped() {
        running.keys.forEach { stop(it, putBack = false) }
    }

    /** Stops every LFO and forgets them all, for a different amp. */
    internal fun clear() {
        dropped()
        kept = emptyMap()
    }

    /** Sends where each running LFO is [at], for an effect that is on, while linked. */
    internal fun tick(at: Duration) {
        if (!panel.linked || panel.holding) return
        val values = panel.state.values
        val due =
            running.mapNotNull { (switch, lfo) ->
                val effect = panel.amp.control(switch) ?: return@mapNotNull null
                if (!effect.isOn(values[switch] ?: 0)) return@mapNotNull null
                val value = lfo.value(at - (started[switch] ?: at))
                if (value == values[lfo.control.id]) null else Triple(switch, lfo, value)
            }
        // One change a tick for every LFO together, taken in turn: the WMI-1 dropped the link when three
        // LFOs each sent one every tick.
        val order = running.keys.toList()
        val after = order.indexOf(lastSent)
        val (switch, lfo, value) = due.minByOrNull { (order.indexOf(it.first) - after - 1).mod(order.size) } ?: return
        lastSent = switch
        panel.set(lfo.control, value)
    }

    private fun run(
        switch: String,
        lfo: Lfo,
    ) {
        freeTime(lfo.control)
        kept = kept + (switch to lfo)
        before = before + (switch to panel.state.values[lfo.control.id])
        started = started + (switch to now())
        running = running + (switch to lfo)
        if (ticking?.isActive == true) return
        ticking =
            scope.launch {
                while (isActive && running.isNotEmpty()) {
                    tick(now())
                    pace()
                }
            }
    }

    private fun stop(
        switch: String,
        putBack: Boolean,
    ) {
        val lfo = running[switch] ?: return
        running = running - switch
        if (putBack && panel.linked) putBack(lfo.control, before[switch])
        before = before - switch
        started = started - switch
        if (running.isEmpty()) ticking?.cancel()
    }

    private fun putBack(
        control: Control,
        value: Int?,
    ) {
        if (value != null && value != panel.state.values[control.id]) panel.set(control, value)
    }

    /** A time swept is not held: the tempo lock lets it go. */
    private fun freeTime(control: Control) {
        if (panel.lock?.control?.controller == control.controller) panel.lock(control, on = false)
    }
}
