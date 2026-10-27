package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import io.github.markovcd.hughandkettle.amp.Microphone

/** The platform's [microphone], and how to [ask] for it: [ask] hands its answer on once the person has given one. */
class MicrophoneAccess(
    val ask: (answer: (Boolean) -> Unit) -> Unit,
    val microphone: Microphone,
)

/** The platform's [MicrophoneAccess], kept for as long as the caller is shown. */
@Composable
internal expect fun rememberMicrophone(): MicrophoneAccess
