package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.PresetNames
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.app.theme.RecordingColors
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.bpm
import io.github.markovcd.hughandkettle.ui.song_record
import io.github.markovcd.hughandkettle.ui.song_record_bar
import io.github.markovcd.hughandkettle.ui.song_record_hint
import io.github.markovcd.hughandkettle.ui.song_record_offline
import io.github.markovcd.hughandkettle.ui.song_record_position
import io.github.markovcd.hughandkettle.ui.song_record_so_far
import io.github.markovcd.hughandkettle.ui.song_record_stop
import io.github.markovcd.hughandkettle.ui.song_record_strip
import io.github.markovcd.hughandkettle.ui.song_record_summary
import io.github.markovcd.hughandkettle.ui.song_record_waits
import io.github.markovcd.hughandkettle.ui.song_record_what
import io.github.markovcd.hughandkettle.ui.song_recording
import io.github.markovcd.hughandkettle.ui.song_stop
import io.github.markovcd.hughandkettle.ui.song_take_discard
import io.github.markovcd.hughandkettle.ui.song_take_saved
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration

/**
 * The Song tab while a take is recorded: "Recording" and the take's name, a ring filling through the bar with
 * the bar and beat in it, and the steps so far, the one sounding last. Tests find the title by
 * `song_recording` and the bar and beat by `song_record_position`.
 */
@Composable
internal fun SongTake(
    takes: TakeState,
    names: PresetNames,
) {
    val take = takes.take ?: return
    val at = takes.at ?: Duration.ZERO
    val beats = (at / take.beat).toInt()
    val steps = take.steps(at)
    RecordingTitle(takes.name, beats)
    RecordingHero(take.tempo, beats, steps.size, at)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.song_record_so_far).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.song_record_waits),
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { index, step ->
            TakenStepRow(index + 1, step, step.preset?.let(names::get), last = index == steps.lastIndex)
        }
    }
}

/** "Recording" in red beside the take's [name], its dot lit on every other beat. */
@Composable
private fun RecordingTitle(
    name: String,
    beats: Int,
) {
    val recording = RecordingColors.current()
    Row(
        Modifier.fillMaxWidth().height(48.dp).testTag("song_recording"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordDot(recording, 12.dp, lit = beats % 2 == 0)
        Text(
            stringResource(Res.string.song_recording),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = recording.mark,
        )
        Text(name, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

/** The bar and beat the take is on, in a ring filling through the bar, beside the tempo, the steps and the time. */
@Composable
private fun RecordingHero(
    tempo: Int,
    beats: Int,
    steps: Int,
    at: Duration,
) {
    val colors = MaterialTheme.colorScheme
    val recording = RecordingColors.current()
    val shape = RoundedCornerShape(16.dp)
    val bar = beats / Song.COUNT_IN_BEATS + 1
    val inBar = beats % Song.COUNT_IN_BEATS + 1
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, recording.edge, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val position = stringResource(Res.string.song_record_position, bar, inBar)
        Box(
            Modifier.size(132.dp).clearAndSetSemantics { contentDescription = position },
            contentAlignment = Alignment.Center,
        ) {
            val track = colors.outlineVariant
            Canvas(Modifier.size(132.dp)) {
                val u = size.width / 132
                drawCircle(track, 58 * u, style = Stroke(8 * u))
                drawArc(
                    recording.mark,
                    -90f,
                    360f * inBar / Song.COUNT_IN_BEATS,
                    false,
                    Offset(8 * u, 8 * u),
                    Size(116 * u, 116 * u),
                    style = Stroke(8 * u, cap = StrokeCap.Round),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.song_record_bar),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    color = colors.onSurfaceVariant,
                )
                Text(
                    "$bar:$inBar",
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("song_record_position"),
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("$tempo ${stringResource(Res.string.bpm)}", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            val seconds = at.inWholeSeconds
            Text(
                stringResource(
                    Res.string.song_record_summary,
                    steps,
                    "${seconds / 60}:${"${seconds % 60}".padStart(2, '0')}",
                ),
                fontSize = 15.sp,
                color = colors.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.song_record_hint),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** Record, outlined red beside Play, and off while not [recordable]. Tests find it by `song_record`. */
@Composable
internal fun RecordButton(
    recordable: Boolean,
    onRecord: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val recording = RecordingColors.current()
    val shape = RoundedCornerShape(16.dp)
    val ink = if (recordable) recording.mark else colors.onSurfaceVariant
    val what = stringResource(Res.string.song_record_what)
    Row(
        modifier
            .height(56.dp)
            .alpha(if (recordable) 1f else DISABLED)
            .clip(shape)
            .border(2.dp, ink, shape)
            .clickable(enabled = recordable, role = Role.Button, onClick = onRecord)
            .semantics { contentDescription = what }
            .testTag("song_record"),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(14.dp).clip(CircleShape).background(ink))
        Text(stringResource(Res.string.song_record), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = ink)
    }
}

/** Stop recording, filled red, across the foot of the Song tab. */
@Composable
internal fun StopRecordingButton(
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val recording = RecordingColors.current()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(recording.mark)
            .clickable(role = Role.Button, onClick = onStop)
            .testTag("song_record_stop"),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(Modifier.size(16.dp)) {
            drawRoundRect(recording.onMark, Offset.Zero, size, CornerRadius(3.dp.toPx()))
        }
        Text(
            stringResource(Res.string.song_record_stop),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = recording.onMark,
        )
    }
}

/**
 * The take just kept, as a new song of [steps] steps, and Discard to throw it away. Tests find it by
 * `song_take_notice` and Discard by `song_take_discard`.
 */
@Composable
internal fun TakeNotice(
    name: String,
    steps: Int,
    onDiscard: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainerHigh)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(start = 14.dp, end = 6.dp)
            .height(52.dp)
            .testTag("song_take_notice"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(Res.string.song_take_saved, name, steps),
            fontSize = 14.sp,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.song_take_discard),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = colors.primary,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(role = Role.Button, onClick = onDiscard)
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .testTag("song_take_discard"),
        )
    }
}

/** Why Record is off: the amp is not linked. */
@Composable
internal fun RecordOffline() {
    Text(
        stringResource(Res.string.song_record_offline),
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp).testTag("song_record_offline"),
    )
}

/**
 * A take going on, under the header of every tab but the Song tab: the take's name, its bar and steps, and Stop.
 * A tap on the strip goes to the Song tab. Tests find it by `song_recording_strip`, and Stop by `song_record_stop`.
 */
@Composable
internal fun RecordingStrip(
    takes: TakeState,
    onOpen: () -> Unit,
) {
    val take = takes.take ?: return
    val recording = RecordingColors.current()
    val at = takes.at ?: Duration.ZERO
    val beats = (at / take.beat).toInt()
    Row(
        Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(recording.ground)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, end = 6.dp)
            .testTag("song_recording_strip"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RecordDot(recording, 12.dp, lit = beats % 2 == 0)
        Text(
            stringResource(Res.string.song_recording),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = recording.mark,
        )
        Text(
            stringResource(
                Res.string.song_record_strip,
                takes.name,
                beats / Song.COUNT_IN_BEATS + 1,
                take.steps(at).size,
            ),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        val shape = RoundedCornerShape(10.dp)
        Text(
            stringResource(Res.string.song_stop),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = recording.mark,
            modifier =
                Modifier
                    .clip(shape)
                    .border(1.dp, recording.mark, shape)
                    .clickable(role = Role.Button, onClick = takes::stop)
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("song_record_stop"),
        )
    }
}

/** The recording dot, full while [lit] and faded between, so it blinks with the beat. */
@Composable
private fun RecordDot(
    recording: RecordingColors,
    size: Dp,
    lit: Boolean,
) {
    Box(
        Modifier
            .size(size)
            .alpha(if (lit) 1f else FADED)
            .clip(CircleShape)
            .background(recording.mark),
    )
}

private const val DISABLED = 0.38f
private const val FADED = 0.3f
