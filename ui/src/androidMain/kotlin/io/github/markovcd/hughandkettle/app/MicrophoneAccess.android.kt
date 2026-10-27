package io.github.markovcd.hughandkettle.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** The phone's microphone, asked for at runtime; the answer waiting on a request is held until Android gives it. */
@Composable
internal actual fun rememberMicrophone(): MicrophoneAccess {
    val context = LocalContext.current.applicationContext
    val waiting = remember { PendingAnswer() }
    val request =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            val answer = waiting.answer
            waiting.answer = null
            answer?.invoke(granted)
        }
    return remember(request) {
        MicrophoneAccess(
            ask = { answer ->
                if (context.allowed()) {
                    answer(true)
                } else {
                    waiting.answer = answer
                    request.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            microphone = PhoneMicrophone(context),
        )
    }
}

private fun Context.allowed(): Boolean =
    checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

private class PendingAnswer {
    var answer: ((Boolean) -> Unit)? = null
}
