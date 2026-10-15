package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream
import java.io.OutputStream

/** The desktop's own file dialog, which holds the window until a file is picked or the dialog is closed. */
@Composable
internal actual fun rememberToneFiles(): ToneFiles =
    remember {
        object : ToneFiles {
            override fun open(read: (() -> InputStream?) -> Unit) {
                picked(FileDialog.LOAD, name = null)?.let { file -> read { file.inputStream() } }
            }

            override fun create(
                name: String,
                write: (() -> OutputStream?) -> Unit,
            ) {
                picked(FileDialog.SAVE, name)?.let { file -> write { file.outputStream() } }
            }
        }
    }

private fun picked(
    mode: Int,
    name: String?,
): File? {
    val dialog = FileDialog(null as Frame?, null, mode).apply { file = name }
    dialog.isVisible = true
    val file = dialog.file ?: return null
    return File(dialog.directory, file)
}
