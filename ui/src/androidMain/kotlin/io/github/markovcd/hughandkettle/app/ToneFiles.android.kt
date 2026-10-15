package io.github.markovcd.hughandkettle.app

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.io.InputStream
import java.io.OutputStream

/** Android's document picker; the callback waiting on a pick is held until the picker answers. */
@Composable
internal actual fun rememberToneFiles(): ToneFiles {
    val resolver = LocalContext.current.contentResolver
    val waiting = remember { Waiting() }
    val export =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(TONE_FILE_TYPE)) { uri ->
            val write = waiting.write
            waiting.write = null
            if (uri != null && write != null) write { resolver.openOutputStream(uri) }
        }
    val import =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            val read = waiting.read
            waiting.read = null
            if (uri != null && read != null) read { resolver.openInputStream(uri) }
        }
    return remember(export, import) {
        object : ToneFiles {
            override fun open(read: (() -> InputStream?) -> Unit) {
                waiting.read = read
                import.launch(TONE_FILE_TYPES)
            }

            override fun create(
                name: String,
                write: (() -> OutputStream?) -> Unit,
            ) {
                waiting.write = write
                export.launch(name)
            }
        }
    }
}

/** What to do with the file the open picker answers with. */
private class Waiting {
    var read: ((() -> InputStream?) -> Unit)? = null
    var write: ((() -> OutputStream?) -> Unit)? = null
}

private const val TONE_FILE_TYPE = "application/json"

// File managers label a .json file in more than one way.
private val TONE_FILE_TYPES = arrayOf("application/json", "application/octet-stream", "text/plain")
