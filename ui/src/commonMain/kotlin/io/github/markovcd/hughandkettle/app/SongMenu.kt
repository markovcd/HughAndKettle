package io.github.markovcd.hughandkettle.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.song_delete
import io.github.markovcd.hughandkettle.ui.song_duplicate
import io.github.markovcd.hughandkettle.ui.song_export
import io.github.markovcd.hughandkettle.ui.song_rename
import io.github.markovcd.hughandkettle.ui.song_row_move_down
import io.github.markovcd.hughandkettle.ui.song_row_move_up
import org.jetbrains.compose.resources.stringResource

/**
 * The ⋮ of the song [name] and its menu: rename, duplicate, move up or down, export and delete. A song that
 * cannot be read only moves and deletes; the untitled song, not kept yet, only renames. Tests find the button
 * by `song_menu:<name>` and the items by `song_rename:<name>` and kin, the moves by `song_row_move_up:<name>`
 * and `song_row_move_down:<name>`, apart from the steps' own arrows.
 */
@Composable
internal fun SongMenu(
    songs: SongState,
    name: String,
    readable: Boolean,
    kept: Boolean,
    description: String,
    onRename: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Box(
            Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) { open = true }
                .semantics { contentDescription = description }
                .testTag("song_menu:$name"),
            contentAlignment = Alignment.Center,
        ) { MenuDots(MaterialTheme.colorScheme.onSurfaceVariant) }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            val rename = Triple("song_rename", Res.string.song_rename, onRename)
            val moves =
                listOf(
                    Triple("song_row_move_up", Res.string.song_row_move_up) { songs.move(name, -1) },
                    Triple("song_row_move_down", Res.string.song_row_move_down) { songs.move(name, 1) },
                )
            val delete = Triple("song_delete", Res.string.song_delete, onDelete)
            val items =
                when {
                    !kept -> {
                        listOf(rename)
                    }

                    !readable -> {
                        moves + delete
                    }

                    else -> {
                        listOf(rename, Triple("song_duplicate", Res.string.song_duplicate) { songs.duplicate(name) }) +
                            moves + listOf(Triple("song_export", Res.string.song_export, onExport), delete)
                    }
                }
            items.forEach { (tag, label, action) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(label),
                            color =
                                if (tag ==
                                    delete.first
                                ) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    },
                    onClick = {
                        open = false
                        action()
                    },
                    modifier = Modifier.testTag("$tag:$name"),
                )
            }
        }
    }
}
