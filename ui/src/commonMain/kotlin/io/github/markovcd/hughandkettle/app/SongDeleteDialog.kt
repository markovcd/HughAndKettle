package io.github.markovcd.hughandkettle.app

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import io.github.markovcd.hughandkettle.amp.KeptSong
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.cancel
import io.github.markovcd.hughandkettle.ui.song_delete
import io.github.markovcd.hughandkettle.ui.song_delete_title
import io.github.markovcd.hughandkettle.ui.song_delete_unreadable
import io.github.markovcd.hughandkettle.ui.song_delete_what
import io.github.markovcd.hughandkettle.ui.song_untitled
import org.jetbrains.compose.resources.stringResource

/** Asks before [kept] is deleted, since it cannot be undone. Tests find the buttons by `song_delete_cancel` and `song_delete_confirm`. */
@Composable
internal fun SongDeleteDialog(
    kept: KeptSong,
    onDelete: () -> Unit,
    onCancel: () -> Unit,
) {
    val song = kept.song
    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Text(
                stringResource(
                    Res.string.song_delete_title,
                    kept.name.ifBlank { stringResource(Res.string.song_untitled) },
                ),
            )
        },
        text = {
            Text(
                if (song == null) {
                    stringResource(Res.string.song_delete_unreadable)
                } else {
                    stringResource(Res.string.song_delete_what, song.steps.size)
                },
            )
        },
        confirmButton = {
            TextButton(onClick = onDelete, modifier = Modifier.testTag("song_delete_confirm")) {
                Text(stringResource(Res.string.song_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel, modifier = Modifier.testTag("song_delete_cancel")) {
                Text(stringResource(Res.string.cancel))
            }
        },
    )
}
