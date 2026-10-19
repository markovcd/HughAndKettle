package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song_cannot_read
import io.github.markovcd.hughandkettle.ui.song_carried
import io.github.markovcd.hughandkettle.ui.song_failed
import io.github.markovcd.hughandkettle.ui.song_import
import io.github.markovcd.hughandkettle.ui.song_imported_as
import io.github.markovcd.hughandkettle.ui.song_list_none
import io.github.markovcd.hughandkettle.ui.song_new
import io.github.markovcd.hughandkettle.ui.song_options_for
import io.github.markovcd.hughandkettle.ui.song_other_amp
import io.github.markovcd.hughandkettle.ui.song_row
import io.github.markovcd.hughandkettle.ui.song_untitled
import io.github.markovcd.hughandkettle.ui.songs
import org.jetbrains.compose.resources.stringResource

/**
 * The songs of the amp as a sheet over the tab, closed by a tap above it: Import and New, what the last change
 * has to tell, and a row per song with its tempo, steps and length, the open one marked. A tap on a row opens
 * that song; one that cannot be read says so and does not open. Tests find the rows by `song_list_rows`, each by
 * `song_open:<name>`, the buttons by `song_import` and `song_new`, a refusal by `song_import_error` and any other
 * word by `song_notice`.
 */
@Composable
internal fun SongSheet(
    songs: SongState,
    onClose: () -> Unit,
    onNew: () -> Unit,
    onImport: () -> Unit,
    onRename: (String) -> Unit,
    onExport: (String) -> Unit,
    onDelete: (KeptSong) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = SCRIM))
            .clickable(remember { MutableInteractionSource() }, indication = null, onClick = onClose),
    ) {
        Column(
            Modifier
                .padding(top = 24.dp)
                .fillMaxSize()
                .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .background(colors.surfaceContainerLow)
                .border(1.dp, colors.outlineVariant, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                .clickable(remember { MutableInteractionSource() }, indication = null) {},
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.songs),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                SheetButton(
                    stringResource(Res.string.song_import),
                    filled = false,
                    onClick = onImport,
                    tag = "song_import",
                )
                SheetButton(stringResource(Res.string.song_new), filled = true, onClick = onNew, tag = "song_new")
            }
            songs.notice?.let { Notice(it, songs::dismiss) }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp)
                    .testTag("song_list_rows"),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (songs.songs.isEmpty()) {
                    Text(stringResource(Res.string.song_list_none), fontSize = 15.sp, color = colors.onSurfaceVariant)
                }
                val open = if (songs.kept) songs.song.name else null
                songs.songs.forEach { kept ->
                    SongRow(
                        songs,
                        kept,
                        open = open != null && kept.name.equals(open, ignoreCase = true),
                        onOpen = {
                            songs.open(kept.name)
                            onClose()
                        },
                        onRename = { onRename(kept.name) },
                        onExport = { onExport(kept.name) },
                        onDelete = { onDelete(kept) },
                    )
                }
            }
        }
    }
}

/** One song: its name over its tempo, steps and length, or over why it cannot be read; a tick when open, and its ⋮. */
@Composable
private fun SongRow(
    songs: SongState,
    kept: KeptSong,
    open: Boolean,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(16.dp)
    val song = kept.song
    val (edge, ground) =
        when {
            song == null -> colors.error.copy(alpha = 0.5f) to colors.errorContainer.copy(alpha = 0.35f)
            open -> colors.primary to colors.primary.copy(alpha = 0.14f)
            else -> colors.outlineVariant to colors.surfaceContainer
        }
    val shown = kept.name.ifBlank { stringResource(Res.string.song_untitled) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(ground)
            .border(1.dp, edge, shape),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier
                .weight(1f)
                .height(64.dp)
                .clickable(enabled = song != null, role = Role.Button, onClick = onOpen)
                .semantics { selected = open }
                .padding(start = 14.dp)
                .testTag("song_open:${kept.name}"),
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically),
        ) {
            Text(
                shown,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = if (kept.name.isBlank()) FontStyle.Italic else FontStyle.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (song == null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    WarningMark(colors.error, 14.dp)
                    Text(stringResource(Res.string.song_cannot_read), fontSize = 13.sp, color = colors.error)
                }
            } else {
                val seconds = song.length.inWholeSeconds
                Text(
                    stringResource(
                        Res.string.song_row,
                        song.tempo,
                        song.steps.size,
                        "${seconds / 60}:${"${seconds % 60}".padStart(2, '0')}",
                    ),
                    fontSize = 13.sp,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) { if (open) CheckMark(colors.primary, 20.dp) }
        SongMenu(
            songs,
            kept.name,
            readable = song != null,
            kept = true,
            description = stringResource(Res.string.song_options_for, shown),
            onRename = onRename,
            onExport = onExport,
            onDelete = onDelete,
        )
    }
}

/** What the last change has to tell: a refusal in red, anything else in amber; a tap puts it away. */
@Composable
private fun Notice(
    notice: SongNotice,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    val refused = notice is SongNotice.OtherAmp || notice is SongNotice.Failed
    val words =
        when (notice) {
            is SongNotice.Carried -> stringResource(Res.string.song_carried, notice.name)
            is SongNotice.ImportedAs -> stringResource(Res.string.song_imported_as, notice.name, notice.kept)
            is SongNotice.OtherAmp -> stringResource(Res.string.song_other_amp, notice.amp)
            is SongNotice.Failed -> stringResource(Res.string.song_failed, notice.reason)
        }
    Row(
        Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
            .fillMaxWidth()
            .clip(shape)
            .background(if (refused) colors.errorContainer.copy(alpha = 0.35f) else colors.primary.copy(alpha = 0.14f))
            .border(1.dp, if (refused) colors.error.copy(alpha = 0.5f) else colors.primary, shape)
            .clickable(onClick = onDismiss)
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .testTag(if (refused) "song_import_error" else "song_notice"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (refused) WarningMark(colors.error, 18.dp)
        Text(words, fontSize = 14.sp, lineHeight = 19.sp, color = if (refused) colors.error else colors.onSurface)
    }
}

@Composable
private fun SheetButton(
    label: String,
    filled: Boolean,
    onClick: () -> Unit,
    tag: String,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier
            .height(44.dp)
            .clip(shape)
            .background(if (filled) colors.primary else colors.surfaceContainerHigh)
            .border(1.dp, if (filled) colors.primary else colors.outlineVariant, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp)
            .testTag(tag),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = if (filled) colors.onPrimary else colors.primary,
        )
    }
}

// The canvas's dimming over what the sheet covers.
private const val SCRIM = 0.62f
