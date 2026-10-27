package io.github.markovcd.hughandkettle.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.markovcd.hughandkettle.amp.ChordListen
import io.github.markovcd.hughandkettle.amp.LoudnessMatch
import io.github.markovcd.hughandkettle.amp.LoudnessMatch.Stage
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.match_again_alone
import io.github.markovcd.hughandkettle.ui.match_again_louder
import io.github.markovcd.hughandkettle.ui.match_again_softer
import io.github.markovcd.hughandkettle.ui.match_play
import io.github.markovcd.hughandkettle.ui.match_play_line
import io.github.markovcd.hughandkettle.ui.match_play_reference_line
import io.github.markovcd.hughandkettle.ui.match_recalling
import io.github.markovcd.hughandkettle.ui.match_recalling_line
import io.github.markovcd.hughandkettle.ui.match_too_loud_by
import io.github.markovcd.hughandkettle.ui.match_too_quiet_by
import io.github.markovcd.hughandkettle.ui.match_wait
import io.github.markovcd.hughandkettle.ui.match_why_loud
import io.github.markovcd.hughandkettle.ui.match_why_noisy
import io.github.markovcd.hughandkettle.ui.match_why_quiet
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** What the cue says and how it looks at each stage. */
internal enum class MatchCue(
    val title: StringResource,
    val pulsing: Boolean,
) {
    Recalling(Res.string.match_recalling, false),
    Play(Res.string.match_play, true),
    Wait(Res.string.match_wait, false),
    Louder(Res.string.match_again_louder, true),
    Softer(Res.string.match_again_softer, true),
    Alone(Res.string.match_again_alone, true),
    ;

    @Composable
    fun ink(): Color =
        when (this) {
            Play -> MaterialTheme.colorScheme.primary
            Recalling, Wait -> MaterialTheme.colorScheme.onSurface
            Louder, Softer, Alone -> MaterialTheme.colorScheme.error
        }

    @Composable
    fun line(running: LoudnessMatch): String =
        when (this) {
            Recalling -> {
                stringResource(Res.string.match_recalling_line)
            }

            Play -> {
                val reference = running.measuringReference
                stringResource(if (reference) Res.string.match_play_reference_line else Res.string.match_play_line)
            }

            Wait -> {
                val off = (running.stage as? Stage.Adjusting)?.off ?: 0.0
                stringResource(
                    if (off >
                        0
                    ) {
                        Res.string.match_too_loud_by
                    } else {
                        Res.string.match_too_quiet_by
                    },
                    decibels(abs(off)),
                )
            }

            Louder -> {
                stringResource(Res.string.match_why_quiet)
            }

            Softer -> {
                stringResource(Res.string.match_why_loud)
            }

            Alone -> {
                stringResource(Res.string.match_why_noisy)
            }
        }

    companion object {
        /** The cue for where [running] is. */
        fun of(running: LoudnessMatch): MatchCue =
            when (val stage = running.stage) {
                is Stage.Playing -> {
                    MatchCue.Play
                }

                is Stage.Adjusting -> {
                    MatchCue.Wait
                }

                is Stage.Retry -> {
                    when (stage.why) {
                        ChordListen.Why.TooQuiet -> MatchCue.Louder
                        ChordListen.Why.TooLoud -> MatchCue.Softer
                        ChordListen.Why.TooNoisy -> MatchCue.Alone
                    }
                }

                else -> {
                    MatchCue.Recalling
                }
            }
    }
}
