package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song_bar
import io.github.markovcd.hughandkettle.ui.song_last
import io.github.markovcd.hughandkettle.ui.song_next
import io.github.markovcd.hughandkettle.ui.song_of_beats
import io.github.markovcd.hughandkettle.ui.song_preset
import io.github.markovcd.hughandkettle.ui.song_step_of
import org.jetbrains.compose.resources.stringResource

/**
 * Where a playing song stands, big enough to read from the floor: a ring filling over the step with its beat
 * in the middle, the step and its preset beside it, a dot for each beat of the bar lighting in turn, and
 * the preset that comes next. Tests find the beat by `song_beat`.
 */
@Composable
internal fun SongBeatHero(
    song: Song,
    at: Song.Position,
    names: PresetNames,
) {
    val colors = MaterialTheme.colorScheme
    val step = song.steps[at.step]
    val shape = RoundedCornerShape(18.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.primary, shape)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(140.dp), contentAlignment = Alignment.Center) {
            val track = colors.outlineVariant
            val ink = colors.primary
            val ring = colors.primary.copy(alpha = 0.55f)
            Canvas(Modifier.size(140.dp)) {
                val u = size.width / 140
                drawCircle(ring, 67 * u, style = Stroke(2 * u))
                drawCircle(track, 58 * u, style = Stroke(10 * u))
                drawArc(
                    ink,
                    -90f,
                    360f * at.beat / step.beats,
                    false,
                    Offset(12 * u, 12 * u),
                    Size(116 * u, 116 * u),
                    style = Stroke(10 * u, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    "${at.beat}",
                    fontSize = 52.sp,
                    lineHeight = 52.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("song_beat"),
                )
                Text(
                    stringResource(Res.string.song_of_beats, step.beats),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.song_step_of, at.step + 1, song.steps.size).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
            Text(songStepTitle(step), fontSize = 26.sp, fontWeight = FontWeight.Bold)
            step.preset?.let(names::get)?.let {
                Text(
                    it,
                    fontSize = 14.sp,
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                Modifier.padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val inBar = (at.beat - 1) % BEATS_A_BAR
                repeat(BEATS_A_BAR) {
                    Box(
                        Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(if (it == inBar) colors.primary else colors.surfaceVariant),
                    )
                }
                Text(
                    stringResource(Res.string.song_bar, (at.beat - 1) / BEATS_A_BAR + 1),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
            Text(
                song.steps.getOrNull(at.step + 1)?.let { stringResource(Res.string.song_next, songStepTitle(it)) }
                    ?: stringResource(Res.string.song_last),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** The bar the dots count: four beats, as a step's length is stepped. */
private const val BEATS_A_BAR = 4
