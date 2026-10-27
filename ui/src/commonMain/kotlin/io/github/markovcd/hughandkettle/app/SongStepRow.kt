package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.app.theme.RecordingColors
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.ok
import io.github.markovcd.hughandkettle.ui.song_beat_count
import io.github.markovcd.hughandkettle.ui.song_beat_fraction
import io.github.markovcd.hughandkettle.ui.song_beats
import io.github.markovcd.hughandkettle.ui.song_beats_range
import io.github.markovcd.hughandkettle.ui.song_beats_title
import io.github.markovcd.hughandkettle.ui.song_less
import io.github.markovcd.hughandkettle.ui.song_more
import io.github.markovcd.hughandkettle.ui.song_move_down
import io.github.markovcd.hughandkettle.ui.song_move_up
import io.github.markovcd.hughandkettle.ui.song_note
import io.github.markovcd.hughandkettle.ui.song_note_picked
import io.github.markovcd.hughandkettle.ui.song_record_beats_so_far
import io.github.markovcd.hughandkettle.ui.song_remove
import io.github.markovcd.hughandkettle.ui.song_step_missing
import io.github.markovcd.hughandkettle.ui.song_step_tone
import org.jetbrains.compose.resources.stringResource

/**
 * One step of a song while it can be changed: a badge saying what it plays, a preset's number in an amber
 * square or a tone's mark in an amber circle, dashed red for a [missing] tone; the preset's name or the
 * tone's, over what it is; its beats stepped to the ends
 * of bars of four or typed in a tap on them, and remove. While [timed], a note button gives the step a note
 * length of its own or leaves it on the one picked. It is moved by dragging, or by its accessibility
 * actions. Tests find it by `song_step:<n>` and its parts by `song_step_kind:<n>`, `song_note:<n>`, `song_beats_down:<n>`, `song_beats:<n>`,
 * `song_beats_up:<n>` and `song_remove:<n>`, counted from 1; the notes offered by `song_note_pick:<id>` and
 * `song_note_pick:none`; the typed beats by `song_beats_field` and `song_beats_done`.
 */
@Composable
internal fun SongStepRow(
    n: Int,
    step: SongStep,
    name: String?,
    missing: Boolean,
    enabled: Boolean,
    last: Boolean,
    timed: Boolean,
    onNote: (NoteLength?) -> Unit,
    onBeats: (Int) -> Unit,
    onMove: (Int) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    var typing by remember { mutableStateOf(false) }
    val up = stringResource(Res.string.song_move_up, n)
    val down = stringResource(Res.string.song_move_down, n)
    if (typing) {
        BeatsDialog(n, step.beats, onDone = {
            onBeats(it)
            typing = false
        }, onCancel = { typing = false })
    }
    Row(
        modifier
            .fillMaxWidth()
            .height(64.dp)
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, if (missing) colors.error else colors.outlineVariant, shape)
            .padding(start = 10.dp, end = 6.dp)
            .semantics {
                customActions =
                    listOfNotNull(
                        CustomAccessibilityAction(up) { true.also { onMove(-1) } }.takeIf { enabled && n > 1 },
                        CustomAccessibilityAction(down) { true.also { onMove(1) } }.takeIf { enabled && !last },
                    )
            }.testTag("song_step:$n"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepKind(n, step, missing)
        StepName(step, name, missing, Modifier.weight(1f))
        if (timed) StepNote(n, step.note, enabled, onNote)
        RoundButton(
            up = false,
            description = stringResource(Res.string.song_less, n),
            enabled = enabled && step.beats > 1,
            onClick = { onBeats(step.shorter().beats) },
            modifier = Modifier.testTag("song_beats_down:$n"),
        )
        Column(
            Modifier
                .width(44.dp)
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, role = Role.Button) { typing = true }
                .testTag("song_beats:$n"),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("${step.beats}", fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            Text(stringResource(Res.string.song_beats), fontSize = 11.sp, color = colors.onSurfaceVariant)
        }
        RoundButton(
            up = true,
            description = stringResource(Res.string.song_more, n),
            enabled = enabled && step.beats < SongStep.MOST_BEATS,
            onClick = { onBeats(step.longer().beats) },
            modifier = Modifier.testTag("song_beats_up:$n"),
        )
        val removing = stringResource(Res.string.song_remove, n)
        Box(
            Modifier
                .size(44.dp)
                .alpha(if (enabled) 1f else DISABLED)
                .clip(CircleShape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onRemove)
                .semantics { contentDescription = removing }
                .testTag("song_remove:$n"),
            contentAlignment = Alignment.Center,
        ) {
            val ink = colors.onSurfaceVariant
            Canvas(Modifier.size(20.dp)) {
                val u = size.width / 24
                drawLine(ink, Offset(6 * u, 6 * u), Offset(18 * u, 18 * u), 2 * u, StrokeCap.Round)
                drawLine(ink, Offset(18 * u, 6 * u), Offset(6 * u, 18 * u), 2 * u, StrokeCap.Round)
            }
        }
    }
}

/**
 * One step of a song while it plays, read only: a played step is ticked and faded, the step playing is
 * amber with its beat and a bar filling along its foot, and a step to come shows its beats. Tests find it
 * by `song_step:<n>`.
 */
@Composable
internal fun PlayedStepRow(
    n: Int,
    step: SongStep,
    name: String?,
    playing: Int,
    beat: Int,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    val now = n - 1 == playing
    val done = n - 1 < playing
    Box(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (done) PLAYED else 1f)
            .clip(shape)
            .background(if (now) colors.primary.copy(alpha = 0.14f) else colors.surfaceContainer)
            .border(1.dp, if (now) colors.primary else colors.outlineVariant, shape)
            .testTag("song_step:$n"),
    ) {
        Row(
            Modifier.fillMaxSize().padding(start = 10.dp, end = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StepNumber(lit = now) {
                when {
                    done -> {
                        val ink = colors.primary
                        Canvas(Modifier.size(16.dp)) {
                            val u = size.width / 24
                            val tick =
                                Path().apply {
                                    moveTo(5 * u, 12 * u)
                                    lineTo(10 * u, 17 * u)
                                    lineTo(19 * u, 7 * u)
                                }
                            drawPath(tick, ink, style = Stroke(3 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        }
                    }

                    now -> {
                        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.onPrimary)
                    }

                    else -> {
                        Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                    }
                }
            }
            PresetName(
                songStepTitle(step),
                name,
                if (now) colors.onPrimaryContainer else colors.onSurfaceVariant,
                Modifier.weight(1f),
            )
            Text(
                if (now) {
                    stringResource(Res.string.song_beat_fraction, beat, step.beats)
                } else {
                    stringResource(Res.string.song_beat_count, step.beats)
                },
                fontSize = 13.sp,
                color = if (now) colors.onPrimaryContainer else colors.onSurfaceVariant,
            )
        }
        if (now) {
            Box(
                Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth(beat.toFloat() / step.beats)
                    .height(3.dp)
                    .background(colors.primary),
            )
        }
    }
}

/**
 * One step of a take being recorded, read only; the [last] is the step sounding now, outlined red with the beats
 * it has lasted so far. Tests find it by `song_step:<n>`.
 */
@Composable
internal fun TakenStepRow(
    n: Int,
    step: SongStep,
    name: String?,
    last: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val recording = RecordingColors.current()
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(shape)
            .background(if (last) recording.ground else colors.surfaceContainer)
            .border(1.dp, if (last) recording.mark else colors.outlineVariant, shape)
            .padding(start = 10.dp, end = 14.dp)
            .testTag("song_step:$n"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepNumber { Text("$n", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary) }
        PresetName(songStepTitle(step), name, colors.onSurfaceVariant, Modifier.weight(1f))
        Text(
            if (last) {
                stringResource(Res.string.song_record_beats_so_far, step.beats)
            } else {
                stringResource(Res.string.song_beat_count, step.beats)
            },
            fontSize = 13.sp,
            fontWeight = if (last) FontWeight.Bold else FontWeight.Normal,
            color = if (last) recording.mark else colors.onSurfaceVariant,
        )
    }
}

/** Asks for a step's beats as a number, taken only when it is 1 to the most a step lasts. */
@Composable
private fun BeatsDialog(
    n: Int,
    beats: Int,
    onDone: (Int) -> Unit,
    onCancel: () -> Unit,
) {
    var text by remember { mutableStateOf("$beats") }
    val typed = text.trim().toIntOrNull()?.takeIf { it in 1..SongStep.MOST_BEATS }
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(stringResource(Res.string.song_beats_title, n)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit).take(3) },
                label = { Text(stringResource(Res.string.song_beats_range, SongStep.MOST_BEATS)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.testTag("song_beats_field"),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { typed?.let(onDone) },
                enabled = typed != null,
                modifier = Modifier.testTag("song_beats_done"),
            ) { Text(stringResource(Res.string.ok)) }
        },
        dismissButton = { TextButton(onClick = onCancel) { Text(stringResource(Res.string.cancel)) } },
    )
}

/** What [step] plays, at a glance: its preset's number in an amber square, or a tone's mark, dashed red when [missing]. */
@Composable
private fun StepKind(
    n: Int,
    step: SongStep,
    missing: Boolean,
) {
    val colors = MaterialTheme.colorScheme
    val tag = Modifier.size(36.dp).testTag("song_step_kind:$n")
    when {
        step.preset != null -> {
            Box(tag.border(1.5.dp, colors.primary, RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(
                    "${step.preset}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.primary,
                    maxLines = 1,
                )
            }
        }

        missing -> {
            Box(tag.dashed(colors.error, 18.dp), contentAlignment = Alignment.Center) {
                TabMark(PanelTab.Tones, colors.error, 18.dp)
            }
        }

        else -> {
            Box(tag.background(colors.primary, CircleShape), contentAlignment = Alignment.Center) {
                TabMark(PanelTab.Tones, colors.onPrimary, 18.dp)
            }
        }
    }
}

/** The preset's name, else "Preset N", or the tone's, struck through when [missing], over what the step is. */
@Composable
private fun StepName(
    step: SongStep,
    name: String?,
    missing: Boolean,
    modifier: Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier) {
        Text(
            if (step.preset != null) name ?: songStepTitle(step) else songStepTitle(step),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            textDecoration = if (missing) TextDecoration.LineThrough else null,
            color = if (missing) colors.onSurfaceVariant else colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            when {
                step.preset != null -> songStepTitle(step).uppercase()
                missing -> stringResource(Res.string.song_step_missing)
                else -> stringResource(Res.string.song_step_tone).uppercase()
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (missing) colors.error else colors.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

/** The step's own note length, amber, or a note mark when it plays the note picked; a tap offers every note. */
@Composable
private fun StepNote(
    n: Int,
    note: NoteLength?,
    enabled: Boolean,
    onNote: (NoteLength?) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val description =
        stringResource(
            Res.string.song_note,
            n,
            note?.let { NoteWords.phrase(it) } ?: stringResource(Res.string.song_note_picked),
        )
    Box {
        Column(
            Modifier
                .width(44.dp)
                .height(44.dp)
                .alpha(if (enabled) 1f else DISABLED)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, if (note != null) colors.primary else colors.outlineVariant, RoundedCornerShape(8.dp))
                .clickable(enabled = enabled, role = Role.Button) { open = true }
                .semantics { contentDescription = description }
                .testTag("song_note:$n"),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (note == null) {
                Text("♪", fontSize = 18.sp, color = colors.onSurfaceVariant)
            } else {
                NoteWords.kind(note)?.let { Text(it, fontSize = 8.sp, color = colors.primary, maxLines = 1) }
                Text(NoteWords.fraction(note), fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.primary)
            }
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.song_note_picked)) },
                onClick = {
                    open = false
                    onNote(null)
                },
                modifier = Modifier.testTag("song_note_pick:none"),
            )
            NoteLength.ALL.forEach { each ->
                DropdownMenuItem(
                    text = {
                        Text(
                            NoteWords.phrase(each),
                            fontWeight = if (each == note) FontWeight.Bold else FontWeight.Normal,
                            color = if (each == note) colors.primary else colors.onSurface,
                        )
                    },
                    onClick = {
                        open = false
                        onNote(each)
                    },
                    modifier = Modifier.testTag("song_note_pick:${each.id}"),
                )
            }
        }
    }
}

/** A step's number in a 28 dp circle, amber when [lit]. */
@Composable
private fun StepNumber(
    lit: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (lit) colors.primary else colors.surfaceContainerHigh),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** A step's [title] in bold with its preset's name beside it. */
@Composable
private fun PresetName(
    title: String,
    name: String?,
    nameColor: Color,
    modifier: Modifier,
) {
    Row(modifier.fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        name?.let {
            Text(
                " $it",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = nameColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private const val DISABLED = 0.38f
private const val PLAYED = 0.6f
