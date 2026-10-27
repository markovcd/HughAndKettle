package io.github.markovcd.hughandkettle.app

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.ChordListen
import io.github.markovcd.hughandkettle.amp.LoudnessMatch
import io.github.markovcd.hughandkettle.amp.LoudnessMatch.Stage
import io.github.markovcd.hughandkettle.amp.MatchResult
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.match_again_alone
import io.github.markovcd.hughandkettle.ui.match_again_louder
import io.github.markovcd.hughandkettle.ui.match_again_softer
import io.github.markovcd.hughandkettle.ui.match_cant_louder
import io.github.markovcd.hughandkettle.ui.match_cant_quieter
import io.github.markovcd.hughandkettle.ui.match_clear_one
import io.github.markovcd.hughandkettle.ui.match_cleared
import io.github.markovcd.hughandkettle.ui.match_close
import io.github.markovcd.hughandkettle.ui.match_count
import io.github.markovcd.hughandkettle.ui.match_discard
import io.github.markovcd.hughandkettle.ui.match_every_named
import io.github.markovcd.hughandkettle.ui.match_in_song
import io.github.markovcd.hughandkettle.ui.match_keep
import io.github.markovcd.hughandkettle.ui.match_kept_note
import io.github.markovcd.hughandkettle.ui.match_louder_than
import io.github.markovcd.hughandkettle.ui.match_make_reference
import io.github.markovcd.hughandkettle.ui.match_matched
import io.github.markovcd.hughandkettle.ui.match_meter
import io.github.markovcd.hughandkettle.ui.match_meter_loud
import io.github.markovcd.hughandkettle.ui.match_meter_quiet
import io.github.markovcd.hughandkettle.ui.match_meter_reference
import io.github.markovcd.hughandkettle.ui.match_mic_denied
import io.github.markovcd.hughandkettle.ui.match_mic_note
import io.github.markovcd.hughandkettle.ui.match_named
import io.github.markovcd.hughandkettle.ui.match_nothing_offered
import io.github.markovcd.hughandkettle.ui.match_off
import io.github.markovcd.hughandkettle.ui.match_play
import io.github.markovcd.hughandkettle.ui.match_play_line
import io.github.markovcd.hughandkettle.ui.match_play_reference_line
import io.github.markovcd.hughandkettle.ui.match_preset_at
import io.github.markovcd.hughandkettle.ui.match_quieter_than
import io.github.markovcd.hughandkettle.ui.match_recalling
import io.github.markovcd.hughandkettle.ui.match_recalling_line
import io.github.markovcd.hughandkettle.ui.match_reference
import io.github.markovcd.hughandkettle.ui.match_reference_note
import io.github.markovcd.hughandkettle.ui.match_running_line
import io.github.markovcd.hughandkettle.ui.match_setup_line
import io.github.markovcd.hughandkettle.ui.match_setup_line_none
import io.github.markovcd.hughandkettle.ui.match_skip
import io.github.markovcd.hughandkettle.ui.match_skipped
import io.github.markovcd.hughandkettle.ui.match_start
import io.github.markovcd.hughandkettle.ui.match_stop
import io.github.markovcd.hughandkettle.ui.match_summary_line
import io.github.markovcd.hughandkettle.ui.match_summary_none
import io.github.markovcd.hughandkettle.ui.match_summary_preset
import io.github.markovcd.hughandkettle.ui.match_summary_skipped
import io.github.markovcd.hughandkettle.ui.match_summary_volume
import io.github.markovcd.hughandkettle.ui.match_tip_alone
import io.github.markovcd.hughandkettle.ui.match_tip_chord
import io.github.markovcd.hughandkettle.ui.match_tip_spot
import io.github.markovcd.hughandkettle.ui.match_title
import io.github.markovcd.hughandkettle.ui.match_too_loud_by
import io.github.markovcd.hughandkettle.ui.match_too_quiet_by
import io.github.markovcd.hughandkettle.ui.match_volume
import io.github.markovcd.hughandkettle.ui.match_volume_to
import io.github.markovcd.hughandkettle.ui.match_wait
import io.github.markovcd.hughandkettle.ui.match_why_loud
import io.github.markovcd.hughandkettle.ui.match_why_noisy
import io.github.markovcd.hughandkettle.ui.match_why_quiet
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * The loudness match as a sheet over the panel: the set-up with its tips, the presets to match and the
 * reference; then the soundcheck, a cue big enough to read from the amp over a meter of the microphone's level
 * against the reference's; then the summary of each preset's correction. Tests find the sheet by `match_sheet`,
 * its close by `match_close`; the set-up by `match_presets`, `match_pick:<n>`, `match_pick_all`,
 * `match_reference:<n>` and `match_start`; the soundcheck by `match_measuring`, `match_cue`, `match_meter`,
 * `match_skip` and `match_stop`; the summary by `match_summary`, each row by `match_result:<n>` with its state
 * `ref`, `ok`, `limit` or `skip`, its clear by `match_clear:<n>`, and its buttons by `match_keep` and
 * `match_discard`.
 */
@Composable
fun MatchOverlay(
    match: MatchState,
    names: PresetNamesState,
) {
    if (match.shown) MatchSheet(match, names, rememberMicrophone())
}

@Composable
private fun MatchSheet(
    match: MatchState,
    names: PresetNamesState,
    access: MicrophoneAccess,
) {
    val colors = MaterialTheme.colorScheme
    val name: @Composable (Int) -> String = { names.names[it] ?: presetWhere(names.names.amp, it) }
    val shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM))
            .clickable(remember { MutableInteractionSource() }, indication = null) {},
    ) {
        Column(
            Modifier
                .padding(top = 24.dp)
                .fillMaxSize()
                .clip(shape)
                .background(colors.surfaceContainerLow)
                .border(1.dp, colors.outlineVariant, shape)
                .testTag("match_sheet"),
        ) {
            val running = match.match
            when {
                running == null -> Setup(match, name, access)
                running.stage == Stage.Done -> Summary(match, running, name)
                else -> Measuring(match, running, name)
            }
        }
    }
}

/** The sheet's title, a line under it and the close. */
@Composable
private fun Header(
    title: String,
    line: String,
    onClose: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(line, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onClose, modifier = Modifier.testTag("match_close")) {
            CrossMark(stringResource(Res.string.match_close))
        }
    }
}

@Composable
private fun ColumnScope.Setup(
    match: MatchState,
    name: @Composable (Int) -> String,
    access: MicrophoneAccess,
) {
    val colors = MaterialTheme.colorScheme
    val reference = match.reference
    Header(
        stringResource(Res.string.match_title),
        if (reference == null) {
            stringResource(Res.string.match_setup_line_none)
        } else {
            stringResource(Res.string.match_setup_line, match.picked.size, name(reference))
        },
        match::close,
    )
    Column(
        Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(Res.string.match_tip_alone, Res.string.match_tip_spot, Res.string.match_tip_chord).forEachIndexed {
            i,
            tip,
            ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(24.dp).background(colors.surfaceContainerHigh, CircleShape), Alignment.Center) {
                    Text("${i + 1}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = colors.primary)
                }
                Text(stringResource(tip), fontSize = 15.sp)
            }
        }
        Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                match.songName?.let { stringResource(Res.string.match_in_song, it) }
                    ?: stringResource(Res.string.match_named),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.match_every_named), fontSize = 14.sp)
            Switch(
                checked = match.everyNamed,
                onCheckedChange = match::pickEveryNamed,
                modifier = Modifier.padding(start = 8.dp).testTag("match_pick_all"),
            )
        }
        Column(Modifier.testTag("match_presets"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            if (match.offered.isEmpty()) {
                Text(
                    stringResource(Res.string.match_nothing_offered),
                    fontSize = 15.sp,
                    color = colors.onSurfaceVariant,
                )
            }
            match.offered.forEach { preset ->
                PickRow(match, preset, name(preset))
            }
        }
        Text(stringResource(Res.string.match_reference_note), fontSize = 13.sp, color = colors.onSurfaceVariant)
        Text(
            stringResource(if (match.denied) Res.string.match_mic_denied else Res.string.match_mic_note),
            fontSize = 13.sp,
            color = if (match.denied) colors.error else colors.onSurfaceVariant,
        )
        match.failure?.let { Text(it, fontSize = 13.sp, color = colors.error) }
    }
    Bottom {
        Button(
            onClick = { match.start(access) },
            enabled = match.startable,
            modifier = Modifier.weight(1f).height(56.dp).testTag("match_start"),
        ) { Text(stringResource(Res.string.match_start), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

/** One preset offered: picked or not, and the star that makes it the reference. */
@Composable
private fun PickRow(
    match: MatchState,
    preset: Int,
    name: String,
) {
    val colors = MaterialTheme.colorScheme
    val picked = preset in match.picked
    val reference = preset == match.reference
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, if (reference) colors.primary else colors.outlineVariant, shape)
            .clickable { match.pick(preset, !picked) }
            .padding(start = 4.dp, end = 4.dp)
            .testTag("match_pick:$preset"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = picked, onCheckedChange = null, modifier = Modifier.padding(12.dp))
        Text("$preset", fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(32.dp))
        Text(name, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (reference) {
            Text(
                stringResource(Res.string.match_reference),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onPrimary,
                modifier =
                    Modifier
                        .clip(
                            RoundedCornerShape(8.dp),
                        ).background(colors.primary)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        IconButton(onClick = { match.refer(preset) }, modifier = Modifier.testTag("match_reference:$preset")) {
            StarMark(reference, stringResource(Res.string.match_make_reference, name))
        }
    }
}

@Composable
private fun ColumnScope.Measuring(
    match: MatchState,
    running: LoudnessMatch,
    name: @Composable (Int) -> String,
) {
    val colors = MaterialTheme.colorScheme
    val preset = running.at ?: return
    val position = running.results.size + 1
    val attempt =
        when (val stage = running.stage) {
            is Stage.Playing -> stage.attempt
            is Stage.Retry -> stage.attempt
            is Stage.Adjusting -> stage.attempt
            else -> 1
        }
    Header(stringResource(Res.string.match_title), stringResource(Res.string.match_running_line), match::close)
    Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            running.presets.indices.forEach { i ->
                val ink =
                    when {
                        i < running.results.size -> colors.primary
                        i == running.results.size -> colors.onSurface
                        else -> colors.outlineVariant
                    }
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(ink),
                )
            }
        }
        Row {
            Text(name(preset), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(
                " " + stringResource(Res.string.match_preset_at, preset),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(Res.string.match_count, position, running.presets.size, attempt),
                fontSize = 14.sp,
                color = colors.onSurfaceVariant,
            )
        }
    }
    Column(
        Modifier.weight(1f).padding(horizontal = 20.dp, vertical = 12.dp).testTag("match_measuring"),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        val cue = MatchCue.of(running)
        Row(horizontalArrangement = Arrangement.spacedBy(18.dp), verticalAlignment = Alignment.CenterVertically) {
            if (cue.pulsing) Pulse(cue.ink())
            Text(
                stringResource(cue.title),
                fontSize = 40.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Bold,
                color = cue.ink(),
                modifier = Modifier.testTag("match_cue"),
            )
        }
        Text(cue.line(running), fontSize = 18.sp, lineHeight = 24.sp)
        Meter(match.level, running.level)
        Row {
            Text(
                stringResource(Res.string.match_meter_quiet),
                fontSize = 12.sp,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            Text(stringResource(Res.string.match_meter_reference), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(Res.string.match_meter_loud),
                fontSize = 12.sp,
                color = colors.error,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.End,
            )
        }
        val stage = running.stage
        Text(
            if (stage is Stage.Adjusting) {
                stringResource(Res.string.match_volume_to, stage.from, stage.to)
            } else {
                running.volume?.let { stringResource(Res.string.match_volume, it) } ?: ""
            },
            fontSize = 14.sp,
            color = colors.onSurfaceVariant,
        )
    }
    Bottom {
        OutlinedButton(onClick = match::skip, modifier = Modifier.weight(1f).height(56.dp).testTag("match_skip")) {
            Text(stringResource(Res.string.match_skip), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedButton(onClick = match::stop, modifier = Modifier.weight(1f).height(56.dp).testTag("match_stop")) {
            Text(stringResource(Res.string.match_stop), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** A dot with a ring that swells and fades, for a cue that wants playing. */
@Composable
private fun Pulse(ink: Color) {
    val swell = rememberInfiniteTransition()
    val grown by swell.animateFloat(0.85f, 1.5f, infiniteRepeatable(tween(PULSE_MS), RepeatMode.Restart))
    Box(Modifier.size(22.dp), Alignment.Center) {
        Box(
            Modifier
                .size(
                    22.dp,
                ).scale(grown)
                .border(3.dp, ink.copy(alpha = (1.5f - grown) / 0.65f * 0.9f), CircleShape),
        )
        Box(Modifier.size(22.dp).background(ink, CircleShape))
    }
}

/**
 * The microphone's [level] as a bar from [FLOOR] dB to full scale, the reference's [reference] level as an
 * outlined band a dB either side, and the last few dB, where the microphone clips, tinted as too loud.
 */
@Composable
private fun Meter(
    level: Double?,
    reference: Double?,
) {
    val colors = MaterialTheme.colorScheme
    val now = fraction(level)
    val description = stringResource(Res.string.match_meter)
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surfaceContainerHighest)
            .semantics { contentDescription = description }
            .testTag("match_meter"),
    ) {
        val width = maxWidth
        Box(Modifier.fillMaxHeight().width(width * now).background(colors.primary))
        reference?.let {
            val low = fraction(it - LoudnessMatch.CLOSE)
            val high = fraction(it + LoudnessMatch.CLOSE)
            Box(
                Modifier
                    .offset(x = width * low)
                    .fillMaxHeight()
                    .width(width * (high - low).coerceAtLeast(0.01f))
                    .border(2.dp, colors.onSurface, RoundedCornerShape(4.dp)),
            )
        }
        val clip = fraction(CLIPPING)
        Box(
            Modifier
                .offset(
                    x = width * clip,
                ).fillMaxHeight()
                .width(width * (1 - clip))
                .background(colors.error.copy(alpha = 0.25f)),
        )
    }
}

private fun fraction(level: Double?): Float = (((level ?: FLOOR) - FLOOR) / -FLOOR).toFloat().coerceIn(0f, 1f)

@Composable
private fun ColumnScope.Summary(
    match: MatchState,
    done: LoudnessMatch,
    name: @Composable (Int) -> String,
) {
    val colors = MaterialTheme.colorScheme
    val others = done.results.filter { it !is MatchResult.Reference }
    val matched = others.count { it is MatchResult.Matched }
    val skipped = others.count { it is MatchResult.Skipped }
    val referenced = done.results.any { it is MatchResult.Reference }
    Header(
        stringResource(Res.string.match_matched, matched, others.size),
        when {
            !referenced -> stringResource(Res.string.match_summary_none)
            skipped > 0 -> stringResource(Res.string.match_summary_skipped, name(done.reference), skipped)
            else -> stringResource(Res.string.match_summary_line, name(done.reference))
        },
        match::close,
    )
    Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(
            stringResource(Res.string.match_summary_preset),
            fontSize = 12.sp,
            color = colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(stringResource(Res.string.match_summary_volume), fontSize = 12.sp, color = colors.onSurfaceVariant)
    }
    Column(
        Modifier
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .testTag("match_summary"),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        done.results.forEach { ResultRow(match, it, name(it.preset)) }
        Text(stringResource(Res.string.match_kept_note), fontSize = 13.sp, color = colors.onSurfaceVariant)
        match.failure?.let { Text(it, fontSize = 13.sp, color = colors.error) }
    }
    Bottom {
        TextButton(onClick = match::close, modifier = Modifier.height(56.dp).testTag("match_discard")) {
            Text(stringResource(Res.string.match_discard), fontSize = 17.sp, fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = match::keep,
            enabled = matched > 0,
            modifier = Modifier.weight(1f).height(56.dp).testTag("match_keep"),
        ) { Text(stringResource(Res.string.match_keep), fontSize = 17.sp, fontWeight = FontWeight.Bold) }
    }
}

/** One preset's outcome: its name, what came of it, and its volume stored and matched. */
@Composable
private fun ResultRow(
    match: MatchState,
    result: MatchResult,
    name: String,
) {
    val colors = MaterialTheme.colorScheme
    val cleared = match.summary[result.preset] == 0 && result !is MatchResult.Reference
    val limit = result is MatchResult.Matched && !result.reached && !cleared
    val skipped = result is MatchResult.Skipped || cleared
    val state =
        when {
            result is MatchResult.Reference -> "ref"
            skipped -> "skip"
            limit -> "limit"
            else -> "ok"
        }
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceContainer)
            .border(1.dp, if (limit) colors.error.copy(alpha = 0.5f) else colors.outlineVariant, shape)
            .padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp)
            .semantics { stateDescription = state }
            .testTag("match_result:${result.preset}"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = if (skipped) colors.onSurfaceVariant else colors.onSurface
        Column(Modifier.weight(1f)) {
            Row {
                Text(
                    name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(" · ${result.preset}", fontSize = 13.sp, color = colors.onSurfaceVariant)
            }
            when {
                result is MatchResult.Reference -> {
                    Text(
                        stringResource(Res.string.match_reference),
                        fontSize = 13.sp,
                        color = colors.primary,
                        fontWeight = FontWeight.Bold,
                    )
                }

                cleared -> {
                    Text(stringResource(Res.string.match_cleared), fontSize = 13.sp, color = colors.onSurfaceVariant)
                }

                result is MatchResult.Skipped -> {
                    Text(stringResource(Res.string.match_skipped), fontSize = 13.sp, color = colors.onSurfaceVariant)
                }

                result is MatchResult.Matched && !result.reached -> {
                    Text(
                        stringResource(
                            if (result.off <
                                0
                            ) {
                                Res.string.match_cant_louder
                            } else {
                                Res.string.match_cant_quieter
                            },
                        ),
                        fontSize = 13.sp,
                        color = colors.error,
                    )
                    Text(
                        stringResource(
                            if (result.off < 0) Res.string.match_quieter_than else Res.string.match_louder_than,
                            decibels(abs(result.off)),
                        ),
                        fontSize = 12.sp,
                        color = colors.error,
                    )
                }

                result is MatchResult.Matched -> {
                    Text(
                        stringResource(Res.string.match_off, decibels(abs(result.off))),
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
        val stored = result.stored
        Text(
            when {
                stored == null -> "–"
                result is MatchResult.Matched && !cleared -> "$stored → ${stored + result.correction}"
                else -> "$stored"
            },
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = ink,
        )
        if (result is MatchResult.Matched && !cleared) {
            IconButton(
                onClick = { match.clear(result.preset) },
                modifier = Modifier.testTag("match_clear:${result.preset}"),
            ) {
                CrossMark(stringResource(Res.string.match_clear_one, name))
            }
        } else {
            Box(Modifier.size(48.dp))
        }
    }
}

/** The sheet's foot: a row of buttons above a rule. */
@Composable
private fun Bottom(content: @Composable RowScope.() -> Unit) {
    Column {
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outlineVariant))
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** [level] dB to one decimal place, as the sheet shows a difference. */
internal fun decibels(level: Double): String = "${(level * 10).roundToInt() / 10}.${(level * 10).roundToInt() % 10}"

private const val SCRIM = 0.62f
private const val PULSE_MS = 1200

// The meter's scale, in dB below full scale: its quiet end, and where the microphone nears clipping.
private const val FLOOR = -70.0
private const val CLIPPING = -3.0
