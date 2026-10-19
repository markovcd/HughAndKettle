package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song_list_open
import io.github.markovcd.hughandkettle.ui.song_locked
import io.github.markovcd.hughandkettle.ui.song_options
import io.github.markovcd.hughandkettle.ui.song_untitled
import org.jetbrains.compose.resources.stringResource

/**
 * The open song's name over the tab: a tap opens the song list, and its ⋮ the song's menu. While a song plays
 * the name is plain, with a padlock saying to stop first. Tests find the name by `song_list`.
 */
@Composable
internal fun SongHeader(
    songs: SongState,
    playing: Boolean,
    onList: () -> Unit,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val song = songs.song
    val shown = song.name.ifBlank { stringResource(Res.string.song_untitled) }
    if (playing) {
        Row(
            Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongName(shown, song.name.isBlank(), Modifier.weight(1f, fill = false).testTag("song_list"))
            Padlock(open = false, color = colors.onSurfaceVariant, size = 16.dp)
            Text(stringResource(Res.string.song_locked), fontSize = 13.sp, color = colors.onSurfaceVariant)
        }
        return
    }
    val description = stringResource(Res.string.song_list_open, shown)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .height(48.dp)
                .clickable(role = Role.Button, onClick = onList)
                .semantics { contentDescription = description }
                .padding(start = 2.dp)
                .testTag("song_list"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SongName(shown, song.name.isBlank(), Modifier.weight(1f, fill = false))
            ChevronDown(colors.onSurfaceVariant, 20.dp)
        }
        SongMenu(
            songs,
            song.name,
            readable = songs.editable,
            kept = songs.kept,
            description = stringResource(Res.string.song_options),
            onRename = onRename,
            onExport = onExport,
            onDelete = onDelete,
        )
    }
}

/** A song's name, big, in italics when it is the untitled one. */
@Composable
private fun SongName(
    shown: String,
    untitled: Boolean,
    modifier: Modifier,
) {
    Text(
        shown,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        fontStyle = if (untitled) FontStyle.Italic else FontStyle.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
