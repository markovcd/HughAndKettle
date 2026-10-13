package io.github.markovcd.hughandkettle.wmi

import io.github.markovcd.hughandkettle.midi.MidiMessage
import java.io.Closeable
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.TimeSource

/**
 * Keeps a link to one WMI-1 up: opens it, and reopens it with [backoff] whenever it fails or drops,
 * until [stop] or [close]. It runs [LinkState] against real [WmiLink]s on a thread of its own and
 * hands every new state to [onState] and every message heard to [onMessage], both on that thread.
 */
public class LinkKeeper(
    private val onState: (LinkState) -> Unit,
    private val onMessage: (MidiMessage) -> Unit = {},
    private val backoff: Backoff = Backoff.Default,
) : Closeable {
    private val inputs = LinkedBlockingQueue<Input>()
    private val origin = TimeSource.Monotonic.markNow()

    @Volatile
    public var state: LinkState = LinkState.Idle
        private set

    @Volatile
    private var link: WmiLink? = null

    // Read and written on the keeper's thread only.
    private var generation = 0
    private var openers = 0
    private var closing = false

    // A link's reader can hear it close before this thread hears it open; that loss waits for the open.
    private var lostEarly: LinkFailure? = null

    private val worker = thread(name = "wmi-link-keeper", isDaemon = true) { run() }

    public fun connect(address: WmiAddress) {
        inputs.put(Input.Asked(LinkEvent.Connect(address)))
    }

    public fun stop() {
        inputs.put(Input.Asked(LinkEvent.Stop))
    }

    /** Sends over the open link; with none, the failure says so. */
    public fun send(message: MidiMessage): Result<Unit> =
        link?.send(message) ?: Result.failure(IllegalStateException("no link to a WMI-1"))

    /** Drops the link and ends the keeper's thread once no open is still in flight. */
    override fun close() {
        inputs.put(Input.Close)
    }

    /** Waits up to [timeout] for the keeper's thread to end after [close]; true when it has. */
    public fun awaitClosed(timeout: Duration): Boolean {
        worker.join(timeout.inWholeMilliseconds.coerceAtLeast(1))
        return !worker.isAlive
    }

    private fun run() {
        while (!closing || openers > 0) {
            val retrying = state as? LinkState.Retrying
            val input =
                if (retrying == null || closing) {
                    inputs.take()
                } else {
                    val wait = (retrying.at - now()).inWholeMilliseconds.coerceAtLeast(0)
                    inputs.poll(wait, TimeUnit.MILLISECONDS) ?: Input.Asked(LinkEvent.Tick)
                }
            when (input) {
                is Input.Asked -> {
                    if (!closing) move(input.event)
                }

                is Input.Opened -> {
                    openers--
                    if (input.generation == generation && !closing) {
                        link = input.link
                        move(LinkEvent.Opened)
                        lostEarly?.let { move(LinkEvent.Failed(it)) }
                    } else {
                        input.link.close()
                    }
                }

                is Input.OpenFailed -> {
                    openers--
                    if (input.generation == generation && !closing) move(LinkEvent.Failed(input.failure))
                }

                is Input.Lost -> {
                    if (input.generation == generation && !closing) {
                        if (state is LinkState.Connecting) {
                            lostEarly = input.failure
                        } else {
                            move(LinkEvent.Failed(input.failure))
                        }
                    }
                }

                Input.Close -> {
                    if (!closing) move(LinkEvent.Stop)
                    closing = true
                }
            }
        }
    }

    private fun move(event: LinkEvent) {
        val before = state
        val after = before.on(event, now(), backoff)
        if (after == before) return
        if (after !is LinkState.Connected) {
            // Anything but the step to connected ends the attempt in hand; late news from it is ignored.
            generation++
            lostEarly = null
            link?.close()
            link = null
        }
        state = after
        if (after is LinkState.Connecting) open(after.address, generation)
        onState(after)
    }

    private fun open(
        address: WmiAddress,
        generation: Int,
    ) {
        openers++
        thread(name = "wmi-link-open", isDaemon = true) {
            WmiLink
                .open(
                    address.host,
                    address.port,
                    onMessage = onMessage,
                    // A null reason is the keeper closing the link itself.
                    onClosed = { reason -> reason?.let { inputs.put(Input.Lost(generation, LinkFailure.Lost(it))) } },
                ).fold(
                    onSuccess = { inputs.put(Input.Opened(generation, it)) },
                    onFailure = { inputs.put(Input.OpenFailed(generation, LinkFailure.of(it))) },
                )
        }
    }

    private fun now(): Duration = origin.elapsedNow()

    private sealed interface Input {
        data class Asked(
            val event: LinkEvent,
        ) : Input

        data class Opened(
            val generation: Int,
            val link: WmiLink,
        ) : Input

        data class OpenFailed(
            val generation: Int,
            val failure: LinkFailure,
        ) : Input

        data class Lost(
            val generation: Int,
            val failure: LinkFailure,
        ) : Input

        data object Close : Input
    }
}
