package io.github.markovcd.hughandkettle.cli

import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.TempoLock
import io.github.markovcd.hughandkettle.midi.MidiMessage
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import io.github.markovcd.hughandkettle.wmi.WmiLink
import java.io.IOException
import java.io.PrintWriter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.Path
import kotlin.time.TimeSource

/**
 * The song of each amp model, the way the app keeps it, in a folder, `--dir` or `~/.hk/songs`: a tempo and
 * a run of presets, each held for its beats. `play` recalls each preset on its beat with the delay time at
 * one repeat a beat, and holds that time across the recalls, as the tempo lock does.
 */
public object SongCommand : Command {
    override val name: String = "song"
    override val usage: String =
        """
        hk song show <amp-id> [--dir D]
        hk song set <amp-id> <bpm> <preset>:<beats> ... [--dir D]
        hk song play <host[:port]> <amp-id> [--dir D]
        """.trimIndent()

    /** What the link's reader hands the loop when the link is gone. */
    private object Ended

    override fun run(
        args: Options,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val verb = args.positional.firstOrNull()
        val rest = args.positional.drop(1)
        val ampAt = if (verb == "play") 1 else 0
        val enough =
            when (verb) {
                "show" -> rest.size == 1
                "set" -> rest.size >= 2
                "play" -> rest.size == 2
                else -> false
            }
        if (!enough) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        val amp =
            AmpArgument.parse(rest[ampAt]).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_ARGUMENTS
            }
        val store = SongStore(args.value("dir")?.let(::Path) ?: Path(System.getProperty("user.home"), ".hk", "songs"))
        if (verb == "set") {
            val song =
                parse(amp, rest.drop(1)).getOrElse {
                    err.println(it.message)
                    return ExitCodes.BAD_ARGUMENTS
                }
            store.save(song).onFailure {
                err.println(it.message)
                return if (it is IOException) ExitCodes.BAD_FILE else ExitCodes.BAD_ARGUMENTS
            }
            print(song, out)
            return ExitCodes.OK
        }
        val song =
            store.song(amp).getOrElse {
                err.println(it.message)
                return ExitCodes.BAD_FILE
            }
        if (verb == "show") {
            print(song, out)
            return ExitCodes.OK
        }
        if (song.steps.isEmpty()) {
            err.println("${amp.id} has no song to play; hk song set makes one")
            return ExitCodes.BAD_ARGUMENTS
        }
        val target = WmiAddress.parse(rest[0])
        if (target == null) {
            err.println(usage)
            return ExitCodes.BAD_ARGUMENTS
        }
        return play(song, target, out, err)
    }

    /** A song from `<bpm> <preset>:<beats> ...`; anything else is reported, not thrown. */
    private fun parse(
        amp: AmpModel,
        words: List<String>,
    ): Result<Song> {
        val tempo =
            words[0].toIntOrNull()?.takeIf { it in Song.TEMPOS }
                ?: return Result.failure(
                    IllegalArgumentException(
                        "a tempo is ${Song.TEMPOS.first}..${Song.TEMPOS.last} BPM, got '${words[0]}'",
                    ),
                )
        val steps =
            words.drop(1).map { word ->
                val parts = word.split(':')
                val preset = parts.getOrNull(0)?.toIntOrNull()?.takeIf { it in 1..amp.presetCount }
                val beats = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..SongStep.MOST_BEATS }
                if (parts.size != 2 || preset == null || beats == null) {
                    return Result.failure(
                        IllegalArgumentException(
                            "a step is <preset>:<beats>, preset 1..${amp.presetCount} and beats 1..${SongStep.MOST_BEATS}, got '$word'",
                        ),
                    )
                }
                SongStep(preset, beats)
            }
        return Result.success(Song(amp, tempo, steps))
    }

    private fun print(
        song: Song,
        out: PrintWriter,
    ) {
        out.println(Json.line("amp" to song.amp.id, "tempo" to song.tempo, "delay_ms" to song.delayMs))
        song.steps.forEachIndexed { index, step ->
            out.println(Json.line("step" to index + 1, "preset" to step.preset, "beats" to step.beats))
        }
    }

    /** Plays [song] on the amp behind [target] from now to its last beat, holding its delay time. */
    private fun play(
        song: Song,
        target: WmiAddress,
        out: PrintWriter,
        err: PrintWriter,
    ): Int {
        val heard = LinkedBlockingQueue<Any>()
        val lostBecause = AtomicReference<String?>()
        val link =
            WmiLink
                .open(
                    target.host,
                    target.port,
                    onMessage = heard::put,
                    onClosed = {
                        lostBecause.set(it)
                        heard.put(Ended)
                    },
                ).getOrElse {
                    err.println("cannot reach $target: ${it.message}")
                    return ExitCodes.NO_LINK
                }
        val time = song.delayTime
        var lock =
            song.amp.controls
                .firstOrNull(Control::timed)
                ?.let { TempoLock(it, time?.value) }
        val started = TimeSource.Monotonic.markNow()

        fun ms() = started.elapsedNow().inWholeMilliseconds
        link.use {
            fun send(messages: List<MidiMessage>): Boolean {
                for (message in messages) {
                    it.send(message).getOrElse { failure ->
                        err.println("send failed: ${failure.message}")
                        return false
                    }
                }
                return true
            }
            var next = 0
            while (true) {
                if (next < song.steps.size && started.elapsedNow() >= song.startOf(next)) {
                    if (!send(song.cue(next))) return ExitCodes.NO_LINK
                    out.println(
                        Json.line(
                            "ms" to ms(),
                            "step" to next + 1,
                            "preset" to song.steps[next].preset,
                            "delay_ms" to song.delayMs,
                        ),
                    )
                    out.flush()
                    next++
                    continue
                }
                val until = if (next < song.steps.size) song.startOf(next) else song.length
                val wait = (until - started.elapsedNow()).inWholeMilliseconds
                if (next >= song.steps.size && wait <= 0) break
                val message = heard.poll(wait.coerceAtLeast(0), TimeUnit.MILLISECONDS) ?: continue
                if (message !is MidiMessage) break
                val held = lock ?: continue
                lock = held.after(message)
                val restore = held.restore(message) ?: continue
                if (!send(listOf(restore))) return ExitCodes.NO_LINK
                out.println(Json.line("ms" to ms(), "restored" to restore.value, "delay_ms" to song.delayMs))
            }
        }
        lostBecause.get()?.let {
            err.println("link lost: $it")
            return ExitCodes.NO_LINK
        }
        out.println(Json.line("ms" to ms(), "ended" to true))
        return ExitCodes.OK
    }
}
