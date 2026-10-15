package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import java.io.InputStream
import java.io.OutputStream

/** Where a tone file is read from and written to: a file the person picks, through the platform's own picker. */
interface ToneFiles {
    /** Asks for a file to read and, once one is picked, hands [read] a way to open it. */
    fun open(read: (() -> InputStream?) -> Unit)

    /** Asks where to write a file named [name] and, once a place is picked, hands [write] a way to open it. */
    fun create(
        name: String,
        write: (() -> OutputStream?) -> Unit,
    )
}

/** The platform's [ToneFiles], kept for as long as the caller is shown. */
@Composable
internal expect fun rememberToneFiles(): ToneFiles
