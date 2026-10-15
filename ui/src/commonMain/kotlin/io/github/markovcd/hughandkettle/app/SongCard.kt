package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.amp.Control
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song
import io.github.markovcd.hughandkettle.ui.song_add
import io.github.markovcd.hughandkettle.ui.song_beat_of
import io.github.markovcd.hughandkettle.ui.song_beats
import io.github.markovcd.hughandkettle.ui.song_bpm
import io.github.markovcd.hughandkettle.ui.song_delay
import io.github.markovcd.hughandkettle.ui.song_less
import io.github.markovcd.hughandkettle.ui.song_more
import io.github.markovcd.hughandkettle.ui.song_none
import io.github.markovcd.hughandkettle.ui.song_play
import io.github.markovcd.hughandkettle.ui.song_remove
import io.github.markovcd.hughandkettle.ui.song_step
import io.github.markovcd.hughandkettle.ui.song_stop
import io.github.markovcd.hughandkettle.ui.song_tempo
import org.jetbrains.compose.resources.stringResource

/**
 * The song: its tempo and the delay time it makes, its steps of a preset held for a number of beats, and play
 * or stop. While it plays, the step playing is bold and counts its beats. Tests find the parts by `song_play`,
 * `song_tempo` and its `song_tempo_down` and `song_tempo_up`, `song_add`, `song_step:<n>` and its
 * `song_beats_down:<n>`, `song_beats_up:<n>` and `song_remove:<n>`, counted from 1.
 */
@Composable
fun SongCard(
    songs: SongState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val song = songs.song
    val position = songs.position
    val editable = songs.editable
    val colors = MaterialTheme.colorScheme
    Surface(modifier = modifier, color = colors.surfaceContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.song),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (position == null) {
                    Button(
                        onClick = songs::start,
                        enabled = enabled && song.steps.isNotEmpty(),
                        modifier = Modifier.testTag("song_play"),
                    ) { Text(stringResource(Res.string.song_play)) }
                } else {
                    Button(onClick = songs::stop, modifier = Modifier.testTag("song_play")) {
                        Text(stringResource(Res.string.song_stop))
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.song_tempo), style = MaterialTheme.typography.labelLarge)
                    delayMs(song.amp.controls.firstOrNull(Control::timed), song.delayTime?.value)?.let {
                        Text(stringResource(Res.string.song_delay, it), style = MaterialTheme.typography.bodySmall)
                    }
                }
                Stepper(
                    text = stringResource(Res.string.song_bpm, song.tempo),
                    enabled = editable,
                    onLess = { songs.tempo(song.tempo - 1) },
                    onMore = { songs.tempo(song.tempo + 1) },
                    tag = "song_tempo",
                )
            }
            if (song.steps.isEmpty()) {
                Text(stringResource(Res.string.song_none), style = MaterialTheme.typography.bodyMedium)
            }
            song.steps.forEachIndexed { index, step ->
                val n = index + 1
                val now = position?.takeIf { it.step == index }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("song_step:$n")) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            stringResource(Res.string.song_step, n, step.preset),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (now != null) FontWeight.Bold else FontWeight.Normal,
                            color = if (now != null) colors.primary else colors.onSurface,
                        )
                        if (now != null) {
                            Text(
                                stringResource(Res.string.song_beat_of, now.beat, step.beats),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                    Stepper(
                        text = stringResource(Res.string.song_beats, step.beats),
                        enabled = editable,
                        onLess = { songs.beats(index, step.beats - BEATS_STEP) },
                        onMore = { songs.beats(index, step.beats + BEATS_STEP) },
                        tag = "song_beats",
                        index = n,
                    )
                    TextButton(
                        onClick = { songs.remove(index) },
                        enabled = editable,
                        modifier = Modifier.testTag("song_remove:$n"),
                    ) {
                        Text(stringResource(Res.string.song_remove))
                    }
                }
            }
            TextButton(onClick = songs::add, enabled = editable, modifier = Modifier.testTag("song_add")) {
                Text(stringResource(Res.string.song_add))
            }
            songs.failure?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = colors.error) }
        }
    }
}

/** A value between a less and a more button; the buttons' tags are `<tag>_down` and `<tag>_up`, with `:<index>` when given. */
@Composable
private fun Stepper(
    text: String,
    enabled: Boolean,
    onLess: () -> Unit,
    onMore: () -> Unit,
    tag: String,
    index: Int? = null,
) {
    val suffix = index?.let { ":$it" } ?: ""
    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedButton(onClick = onLess, enabled = enabled, modifier = Modifier.testTag("${tag}_down$suffix")) {
            Text(stringResource(Res.string.song_less))
        }
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(min = 72.dp).testTag("$tag$suffix"),
        )
        OutlinedButton(onClick = onMore, enabled = enabled, modifier = Modifier.testTag("${tag}_up$suffix")) {
            Text(stringResource(Res.string.song_more))
        }
    }
}

/** The milliseconds a time knob at [value] stands for. */
private fun delayMs(
    time: Control?,
    value: Int?,
): Int? = time?.scale?.let { scale -> value?.let(scale::amount) }

/** How far one press moves a step's length: one bar of four. */
private const val BEATS_STEP = 4
