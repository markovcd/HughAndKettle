package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.amp.Song
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.bpm
import io.github.markovcd.hughandkettle.ui.song_add
import io.github.markovcd.hughandkettle.ui.song_add_what
import io.github.markovcd.hughandkettle.ui.song_count_in
import io.github.markovcd.hughandkettle.ui.song_count_in_what
import io.github.markovcd.hughandkettle.ui.song_create
import io.github.markovcd.hughandkettle.ui.song_held
import io.github.markovcd.hughandkettle.ui.song_new_title
import io.github.markovcd.hughandkettle.ui.song_none
import io.github.markovcd.hughandkettle.ui.song_none_now
import io.github.markovcd.hughandkettle.ui.song_off
import io.github.markovcd.hughandkettle.ui.song_play
import io.github.markovcd.hughandkettle.ui.song_rename
import io.github.markovcd.hughandkettle.ui.song_rename_title
import io.github.markovcd.hughandkettle.ui.song_steps
import io.github.markovcd.hughandkettle.ui.song_stop
import io.github.markovcd.hughandkettle.ui.song_summary
import io.github.markovcd.hughandkettle.ui.song_then
import io.github.markovcd.hughandkettle.ui.song_unreadable
import io.github.markovcd.hughandkettle.ui.song_unsaved
import org.jetbrains.compose.resources.stringResource

/**
 * The song's own tab. Stopped: its tempo, its count-in, its steps to lengthen, shorten or remove, a step to
 * add on the preset the amp is on, and Play along the foot. Counting in: the clicks left, big. Playing: the
 * beat, big, over the steps read only, and Stop. A song that cannot be read says so on top and can be neither
 * changed nor played. Tests find Play and Stop by `song_play`, adding a step by `song_add`, the count-in's
 * switch by `song_count_in` and the clicks left by `song_countdown`. The open song's name heads the tab and
 * opens the list of the amp's songs over it.
 */
@Composable
fun SongTab(
    songs: SongState,
    presetNames: PresetNamesState,
    preset: Int?,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val song = songs.song
    val at = songs.position
    val names = presetNames.names
    val countdown = songs.countdown
    val readable = at != null || countdown != null || songs.editable
    val playing = at != null || countdown != null
    val files = rememberToneFiles()
    var listing by remember { mutableStateOf(false) }
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<KeptSong?>(null) }
    val export = { name: String -> files.create("${name.ifBlank { "song" }}.song.json") { songs.export(name, it) } }
    val shown = listing && !playing
    Box(modifier) {
        Column(Modifier.fillMaxSize().then(if (shown) Modifier.clearAndSetSemantics {} else Modifier)) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(if (at == null) 12.dp else 10.dp),
            ) {
                SongHeader(
                    songs,
                    playing,
                    onList = { listing = true },
                    onRename = { renaming = song.name },
                    onExport = { export(song.name) },
                    onDelete = { deleting = songs.songs.firstOrNull { it.name.equals(song.name, ignoreCase = true) } },
                )
                songs.failure?.let { SongAlert(readable, it) }
                if (countdown != null) {
                    SongCountdown(countdown, song)
                } else if (at != null) {
                    SongBeatHero(song, at, names)
                    if (song.delayMs != null) SongHeldStrip(song)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        song.steps.forEachIndexed { index, step ->
                            PlayedStepRow(index + 1, step, names[step.preset], at.step, at.beat)
                        }
                    }
                } else {
                    SongTempoCard(songs, readable)
                    CountInRow(song.countIn, songs.editable, songs::countIn)
                    StepsHeader(song)
                    if (song.steps.isEmpty()) {
                        NoSteps(preset, songs.editable, songs::add)
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(STEP_GAP)) {
                            val drag = remember { StepDrag() }
                            song.steps.forEachIndexed { index, step ->
                                SongStepRow(
                                    index + 1,
                                    step,
                                    names[step.preset],
                                    songs.editable,
                                    last = index == song.steps.lastIndex,
                                    onBeats = { songs.beats(index, it) },
                                    onMove = { songs.move(index, index + it) },
                                    onRemove = { songs.remove(index) },
                                    modifier = Modifier.dragged(drag, index, songs),
                                )
                            }
                            AddStep(preset ?: 1, songs.editable, songs::add)
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PlayBar(
                playing = playing,
                enabled = enabled && readable && song.steps.isNotEmpty(),
                onPlay = songs::start,
                onStop = songs::stop,
            )
        }
        if (shown) {
            SongSheet(
                songs,
                onClose = {
                    listing = false
                    songs.dismiss()
                },
                onNew = { creating = true },
                onImport = { files.open { songs.import(it) } },
                onRename = { renaming = it },
                onExport = export,
                onDelete = { deleting = it },
            )
        }
    }
    if (creating) {
        SongNameDialog(
            title = stringResource(Res.string.song_new_title),
            done = stringResource(Res.string.song_create),
            tag = "song_new",
            doneTag = "song_new_create",
            initial = "",
            taken = songs::taken,
            onDone = {
                creating = false
                listing = false
                songs.create(it)
            },
            onCancel = { creating = false },
        )
    }
    renaming?.let { name ->
        SongNameDialog(
            title = stringResource(Res.string.song_rename_title),
            done = stringResource(Res.string.song_rename),
            tag = "song_rename",
            doneTag = "song_rename_done",
            initial = name,
            taken = songs::taken,
            onDone = {
                renaming = null
                songs.rename(name, it)
            },
            onCancel = { renaming = null },
        )
    }
    deleting?.let { kept ->
        SongDeleteDialog(
            kept,
            onDelete = {
                deleting = null
                songs.delete(kept.name)
            },
            onCancel = { deleting = null },
        )
    }
}

/** The count-in, switched on or off. */
@Composable
private fun CountInRow(
    on: Boolean,
    enabled: Boolean,
    onCountIn: (Boolean) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.song_count_in), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.song_count_in_what), fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        Switch(
            checked = on,
            onCheckedChange = onCountIn,
            enabled = enabled,
            modifier = Modifier.testTag("song_count_in"),
        )
    }
}

/** The clicks of the count-in still to come, big, in a ring that fills as they pass, and the preset that starts. */
@Composable
private fun SongCountdown(
    left: Int,
    song: Song,
) {
    val colors = MaterialTheme.colorScheme
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
            val counted = Song.COUNT_IN_BEATS - left + 1
            Canvas(Modifier.size(140.dp)) {
                val u = size.width / 140
                drawCircle(track, 58 * u, style = Stroke(10 * u))
                drawArc(
                    ink,
                    -90f,
                    360f * counted / Song.COUNT_IN_BEATS,
                    false,
                    Offset(12 * u, 12 * u),
                    Size(116 * u, 116 * u),
                    style = Stroke(10 * u, cap = StrokeCap.Round),
                )
            }
            Text(
                "$left",
                fontSize = 64.sp,
                lineHeight = 64.sp,
                fontWeight = FontWeight.Bold,
                color = colors.primary,
                modifier = Modifier.testTag("song_countdown"),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.song_count_in).uppercase(),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.song_then, song.steps.first().preset),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
            )
            Text("${song.tempo} ${stringResource(Res.string.bpm)}", fontSize = 14.sp, color = colors.onSurfaceVariant)
        }
    }
}

/** "Steps", and how many with how many beats lasting how long. */
@Composable
private fun StepsHeader(song: Song) {
    val colors = MaterialTheme.colorScheme
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(Res.string.song_steps).uppercase(),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        if (song.steps.isNotEmpty()) {
            val seconds = song.length.inWholeSeconds
            Text(
                stringResource(
                    Res.string.song_summary,
                    song.steps.size,
                    song.steps.sumOf(SongStep::beats),
                    "${seconds / 60}:${"${seconds % 60}".padStart(2, '0')}",
                ),
                fontSize = 13.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

/** A song with no steps: a dashed box saying what to add, with the preset the amp is on, and Add step. */
@Composable
private fun NoSteps(
    preset: Int?,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else UNREADABLE)
            .dashed(colors.surfaceVariant, 16.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Note(colors.onSurfaceVariant)
        Text(
            preset?.let { stringResource(Res.string.song_none_now, it) } ?: stringResource(Res.string.song_none),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        val shape = RoundedCornerShape(12.dp)
        Row(
            Modifier
                .height(48.dp)
                .clip(shape)
                .background(if (enabled) colors.primary else colors.surfaceContainerHigh)
                .clickable(enabled = enabled, role = Role.Button, onClick = onAdd)
                .padding(horizontal = 22.dp)
                .testTag("song_add"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val ink = if (enabled) colors.onPrimary else colors.onSurfaceVariant
            Plus(ink)
            Text(stringResource(Res.string.song_add), fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ink)
        }
    }
}

/** Add step, under the steps, saying the preset and beats it adds. */
@Composable
private fun AddStep(
    preset: Int,
    enabled: Boolean,
    onAdd: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else DISABLED)
            .dashed(colors.surfaceVariant, 14.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(enabled = enabled, role = Role.Button, onClick = onAdd)
            .testTag("song_add"),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Plus(colors.primary)
        Text(
            stringResource(Res.string.song_add),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.primary,
        )
        Text(
            stringResource(Res.string.song_add_what, preset, SongState.NEW_STEP_BEATS),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = colors.onSurfaceVariant,
        )
    }
}

/** Play, filled amber, or Stop, outlined, across the foot of the tab. */
@Composable
private fun PlayBar(
    playing: Boolean,
    enabled: Boolean,
    onPlay: () -> Unit,
    onStop: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val ground =
        when {
            playing -> colors.primary.copy(alpha = 0.14f)
            enabled -> colors.primary
            else -> colors.surfaceContainerHigh
        }
    val ink =
        when {
            playing -> colors.onPrimaryContainer
            enabled -> colors.onPrimary
            else -> colors.onSurfaceVariant
        }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .height(56.dp)
            .clip(shape)
            .background(ground)
            .then(if (playing) Modifier.border(2.dp, colors.primary, shape) else Modifier)
            .clickable(enabled = playing || enabled, role = Role.Button, onClick = if (playing) onStop else onPlay)
            .testTag("song_play"),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val mark = if (playing) colors.primary else ink
        Canvas(Modifier.size(20.dp)) {
            val u = size.width / 24
            if (playing) {
                drawRoundRect(mark, Offset(5 * u, 5 * u), Size(14 * u, 14 * u), CornerRadius(2 * u))
            } else {
                val triangle =
                    Path().apply {
                        moveTo(7 * u, 4 * u)
                        lineTo(20 * u, 12 * u)
                        lineTo(7 * u, 20 * u)
                        close()
                    }
                drawPath(triangle, mark)
            }
        }
        Text(
            stringResource(if (playing) Res.string.song_stop else Res.string.song_play),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
    }
}

/** What went wrong with the song: it cannot be read, so it is off, or it could not be saved. */
@Composable
private fun SongAlert(
    readable: Boolean,
    message: String,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.errorContainer.copy(alpha = 0.35f))
            .border(1.dp, colors.error.copy(alpha = 0.5f), shape)
            .padding(14.dp)
            .testTag("song_failure"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val ink = colors.error
        WarningMark(ink, 22.dp, Modifier.padding(top = 1.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(if (readable) Res.string.song_unsaved else Res.string.song_unreadable),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = ink,
            )
            if (!readable) Text(stringResource(Res.string.song_off), fontSize = 14.sp, color = ink)
            Text(message, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}

/** The tempo lock holding the song's delay time, under the beat while the song plays. */
@Composable
private fun SongHeldStrip(song: Song) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(shape)
            .background(colors.surfaceContainerLow)
            .border(1.dp, colors.outlineVariant, shape)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Padlock(open = false, color = colors.primary, size = 16.dp)
        Text(
            stringResource(Res.string.song_held, song.delayMs ?: 0),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.onPrimaryContainer,
            modifier = Modifier.weight(1f),
        )
        Text("${song.tempo} ${stringResource(Res.string.bpm)}", fontSize = 14.sp, color = colors.onSurfaceVariant)
    }
}

@Composable
private fun Plus(ink: Color) {
    Canvas(Modifier.size(18.dp)) {
        val u = size.width / 24
        drawLine(ink, Offset(5 * u, 12 * u), Offset(19 * u, 12 * u), 2.5f * u, StrokeCap.Round)
        drawLine(ink, Offset(12 * u, 5 * u), Offset(12 * u, 19 * u), 2.5f * u, StrokeCap.Round)
    }
}

/** The song's glyph: a pair of beamed notes. */
@Composable
private fun Note(ink: Color) {
    TabMark(PanelTab.Song, ink, 32.dp)
}

/**
 * A step lifted by a long press and dragged up or down the list, trading places with each step it passes
 * halfway over. The gesture stays with the place the press began in, so [drag] says which step is lifted.
 */
@Composable
private fun Modifier.dragged(
    drag: StepDrag,
    index: Int,
    songs: SongState,
): Modifier {
    val gap = with(LocalDensity.current) { STEP_GAP.toPx() }
    var height by remember { mutableIntStateOf(0) }
    return onSizeChanged { height = it.height }
        .zIndex(if (drag.step == index) 1f else 0f)
        .graphicsLayer {
            if (drag.step == index) {
                translationY = drag.lift
                shadowElevation = 8.dp.toPx()
            }
        }.pointerInput(Unit) {
            detectDragGesturesAfterLongPress(
                onDragStart = {
                    if (songs.editable) drag.step = index
                    drag.lift = 0f
                },
                onDrag = { change, amount ->
                    val at = drag.step ?: return@detectDragGesturesAfterLongPress
                    change.consume()
                    drag.lift += amount.y
                    val row = height + gap
                    val last = songs.song.steps.lastIndex
                    if (drag.lift > row / 2 && at < last) {
                        songs.move(at, at + 1)
                        drag.step = at + 1
                        drag.lift -= row
                    } else if (drag.lift < -row / 2 && at > 0) {
                        songs.move(at, at - 1)
                        drag.step = at - 1
                        drag.lift += row
                    }
                },
                onDragEnd = { drag.drop() },
                onDragCancel = { drag.drop() },
            )
        }
}

/** A dashed outline of [color] with rounded corners. */
private fun Modifier.dashed(
    color: Color,
    radius: Dp,
): Modifier =
    drawBehind {
        drawRoundRect(
            color,
            cornerRadius = CornerRadius(radius.toPx()),
            style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
        )
    }

private val STEP_GAP = 8.dp
private const val UNREADABLE = 0.6f
private const val DISABLED = 0.38f
