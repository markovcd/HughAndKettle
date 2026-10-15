package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.bpm
import io.github.markovcd.hughandkettle.ui.song_delay
import io.github.markovcd.hughandkettle.ui.song_no_delay
import io.github.markovcd.hughandkettle.ui.song_tap
import io.github.markovcd.hughandkettle.ui.song_tempo
import io.github.markovcd.hughandkettle.ui.song_tempo_down
import io.github.markovcd.hughandkettle.ui.song_tempo_up
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.nanoseconds

/**
 * The song's tempo, big, with the delay time it makes, stepped down and up by one or tapped. A song that
 * could not be read shows "—" and dims. Tests find the tempo by `song_tempo`, the buttons by `song_tempo_down`,
 * `song_tap` and `song_tempo_up`.
 */
@Composable
internal fun SongTempoCard(
    songs: SongState,
    readable: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val song = songs.song
    val editable = songs.editable
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .alpha(if (readable) 1f else UNREADABLE)
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(Res.string.song_tempo).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    if (readable) "${song.tempo}" else UNKNOWN,
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("song_tempo"),
                )
                Text(
                    stringResource(Res.string.bpm),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            Text(
                song.delayMs?.takeIf { readable }?.let { stringResource(Res.string.song_delay, it) }
                    ?: stringResource(Res.string.song_no_delay),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
        RoundButton(
            up = false,
            description = stringResource(Res.string.song_tempo_down),
            enabled = editable && song.tempo > Song.TEMPOS.first,
            onClick = { songs.tempo(song.tempo - 1) },
            modifier = Modifier.testTag("song_tempo_down"),
        )
        TapButton(editable) { songs.tap(System.nanoTime().nanoseconds) }
        RoundButton(
            up = true,
            description = stringResource(Res.string.song_tempo_up),
            enabled = editable && song.tempo < Song.TEMPOS.last,
            onClick = { songs.tempo(song.tempo + 1) },
            modifier = Modifier.testTag("song_tempo_up"),
        )
    }
}

@Composable
private fun TapButton(
    enabled: Boolean,
    onTap: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .size(width = 64.dp, height = 44.dp)
            .alpha(if (enabled) 1f else DISABLED)
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onTap)
            .testTag("song_tap"),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(Res.string.song_tap),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
        )
    }
}

private const val UNKNOWN = "—"
private const val UNREADABLE = 0.45f
private const val DISABLED = 0.38f
