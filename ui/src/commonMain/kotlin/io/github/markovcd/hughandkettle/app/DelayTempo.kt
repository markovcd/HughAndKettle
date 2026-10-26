package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.NoteReach
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.hint_lock
import io.github.markovcd.hughandkettle.ui.note_held_longest
import io.github.markovcd.hughandkettle.ui.note_held_shortest
import io.github.markovcd.hughandkettle.ui.note_length
import io.github.markovcd.hughandkettle.ui.note_too_long
import io.github.markovcd.hughandkettle.ui.note_too_long_alert
import io.github.markovcd.hughandkettle.ui.note_too_short
import io.github.markovcd.hughandkettle.ui.note_too_short_alert
import io.github.markovcd.hughandkettle.ui.tempo_at
import io.github.markovcd.hughandkettle.ui.tempo_from_amp
import io.github.markovcd.hughandkettle.ui.tempo_held
import io.github.markovcd.hughandkettle.ui.tempo_lock
import io.github.markovcd.hughandkettle.ui.tempo_lock_off
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * The delay's tempo lock, over its knobs: off, a line saying each preset brings its own time; on, the note it
 * repeats at and the song's tempo, a warning while that note is past the amp's reach, and the notes to pick
 * from, any out of reach at this tempo disabled. Each preset the lock answers lights the button with "Held"
 * for a second. Tests find the lock by `lock:delay_time`, the note shown by `value:delay_time`, the tempo by
 * `fx_tempo` and each note by `note_length:<id>`.
 */
@Composable
internal fun DelayTempo(
    songs: SongState,
    panel: PanelState,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val locked = songs.timed
    val song = songs.song
    val note = songs.sounding
    val reach = song.reach(note)
    var held by remember { mutableStateOf(false) }
    var answered by remember { mutableStateOf(panel.restored) }
    LaunchedEffect(panel.restored) {
        if (panel.restored == answered) return@LaunchedEffect
        answered = panel.restored
        held = true
        delay(1.seconds)
        held = false
    }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Hint(stringResource(Res.string.hint_lock)) {
                LockButton(locked, held, enabled, songs::lock, Modifier.testTag("lock:delay_time"))
            }
            if (!locked) {
                Text(
                    stringResource(Res.string.tempo_lock_off),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.weight(1f),
                )
            } else {
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        when (reach) {
                            NoteReach.TooLong -> stringResource(Res.string.note_held_longest)
                            NoteReach.TooShort -> stringResource(Res.string.note_held_shortest)
                            else -> NoteWords.phrase(note)
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End,
                        modifier = Modifier.testTag("value:delay_time"),
                    )
                    Text(
                        if (songs.tempoFromAmp) {
                            stringResource(Res.string.tempo_from_amp, song.tempo)
                        } else {
                            stringResource(Res.string.tempo_at, song.tempo)
                        },
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.testTag("fx_tempo"),
                    )
                }
            }
        }
        if (locked) {
            if (reach == NoteReach.TooLong || reach == NoteReach.TooShort) {
                val alert =
                    if (reach ==
                        NoteReach.TooLong
                    ) {
                        Res.string.note_too_long_alert
                    } else {
                        Res.string.note_too_short_alert
                    }
                Text(
                    stringResource(alert, NoteWords.phrase(note), song.tempo),
                    fontSize = 13.sp,
                    color = colors.onErrorContainer,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.errorContainer)
                            .padding(10.dp)
                            .testTag("note_alert"),
                )
            }
            NoteGrid(songs, enabled)
        }
    }
}

/** The notes in rows of four, the one picked lit; a note out of reach at the song's tempo says so and, unless picked, is disabled. */
@Composable
private fun NoteGrid(
    songs: SongState,
    enabled: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(Res.string.note_length).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onSurfaceVariant,
        )
        NoteLength.ALL.chunked(NOTES_PER_ROW).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { note ->
                    NoteButton(
                        note,
                        songs.note == note,
                        songs.song.reach(note),
                        enabled,
                        { songs.note(note) },
                        Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NoteButton(
    note: NoteLength,
    picked: Boolean,
    reach: NoteReach?,
    enabled: Boolean,
    onPick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(10.dp)
    val fits = reach == null || reach == NoteReach.Fits
    val usable = enabled && (fits || picked)
    val phrase = NoteWords.phrase(note)
    val beyond =
        when (reach) {
            NoteReach.TooLong -> stringResource(Res.string.note_too_long)
            NoteReach.TooShort -> stringResource(Res.string.note_too_short)
            else -> null
        }
    Column(
        modifier
            .height(56.dp)
            .alpha(if (usable) 1f else DISABLED)
            .clip(shape)
            .background(if (picked) colors.primary else colors.surfaceContainerHigh)
            .border(1.dp, if (picked) colors.primary else colors.outlineVariant, shape)
            .toggleable(value = picked, enabled = usable, role = Role.RadioButton, onValueChange = { onPick() })
            .semantics {
                contentDescription = listOfNotNull(phrase, beyond).joinToString(", ")
                if (picked && beyond != null) error(beyond)
            }.testTag("note_length:${note.id}"),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val ink = if (picked) colors.onPrimary else colors.onSurface
        Text(
            NoteWords.kind(note) ?: "",
            fontSize = 11.sp,
            color = if (picked) colors.onPrimary else colors.onSurfaceVariant,
        )
        Text(NoteWords.fraction(note), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = ink)
        if (beyond != null) Text(beyond, fontSize = 10.sp, color = ink)
    }
}

/** The tempo lock as a toggle button: an open grey padlock while off, amber while on, solid amber reading "Held" when [held]. */
@Composable
private fun LockButton(
    locked: Boolean,
    held: Boolean,
    enabled: Boolean,
    onLock: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val ground =
        when {
            held -> colors.primary
            locked -> colors.primary.copy(alpha = 0.16f)
            else -> colors.surfaceContainer
        }
    val ink =
        when {
            held -> colors.onPrimary
            locked -> colors.primary
            else -> colors.onSurfaceVariant
        }
    val label =
        when {
            held -> colors.onPrimary
            locked -> colors.onPrimaryContainer
            else -> colors.onSurface
        }
    Row(
        modifier
            .height(44.dp)
            .clip(shape)
            .background(ground)
            .border(1.dp, if (locked) colors.primary else colors.outlineVariant, shape)
            .toggleable(value = locked, enabled = enabled, role = Role.Switch, onValueChange = onLock)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Padlock(open = !locked, color = ink)
        Text(
            stringResource(if (held) Res.string.tempo_held else Res.string.tempo_lock),
            style = MaterialTheme.typography.labelLarge,
            color = label,
            fontWeight = FontWeight.Bold,
        )
    }
}

private const val NOTES_PER_ROW = 4
private const val DISABLED = 0.38f
