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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.SongStep
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
import io.github.markovcd.hughandkettle.ui.song_remove
import org.jetbrains.compose.resources.stringResource

/**
 * One step of a song while it can be changed: its preset and the preset's name, its beats stepped to the ends
 * of bars of four or typed in a tap on them, and remove. It is moved by dragging, or by its accessibility
 * actions. Tests find it by `song_step:<n>` and its parts by `song_beats_down:<n>`, `song_beats:<n>`,
 * `song_beats_up:<n>` and `song_remove:<n>`, counted from 1; the typed beats by `song_beats_field` and
 * `song_beats_done`.
 */
@Composable
internal fun SongStepRow(
    n: Int,
    step: SongStep,
    name: String?,
    enabled: Boolean,
    last: Boolean,
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
            .border(1.dp, colors.outlineVariant, shape)
            .padding(start = 14.dp, end = 6.dp)
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
        PresetName(songStepTitle(step), name, colors.onSurfaceVariant, Modifier.weight(1f))
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
                inline = true,
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

/** A step's [title] in bold with its preset's name under it, or beside it when [inline]. */
@Composable
private fun PresetName(
    title: String,
    name: String?,
    nameColor: Color,
    modifier: Modifier,
    inline: Boolean = false,
) {
    if (inline) {
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
    } else {
        Column(modifier) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            name?.let { Text(it, fontSize = 13.sp, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}

private const val DISABLED = 0.38f
private const val PLAYED = 0.6f
