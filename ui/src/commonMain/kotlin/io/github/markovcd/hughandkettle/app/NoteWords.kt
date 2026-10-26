package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.ui.Res
import io.github.markovcd.hughandkettle.ui.note_dotted
import io.github.markovcd.hughandkettle.ui.note_triplet
import org.jetbrains.compose.resources.stringResource

/** How a note length reads: its kind ("Dotted", "Triplet" or none) over its fraction, and the two as one phrase. */
internal object NoteWords {
    /** The note's fraction, `1/8` for a dotted or triplet eighth too. */
    fun fraction(note: NoteLength): String = note.id.trimEnd('d', 't')

    /** "Dotted" or "Triplet", or null for a plain note. */
    @Composable
    fun kind(note: NoteLength): String? =
        when (note.id.last()) {
            'd' -> stringResource(Res.string.note_dotted)
            't' -> stringResource(Res.string.note_triplet)
            else -> null
        }

    /** The note as a phrase: "Dotted 1/8", "1/4". */
    @Composable
    fun phrase(note: NoteLength): String = listOfNotNull(kind(note), fraction(note)).joinToString(" ")
}
