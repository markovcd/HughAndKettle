package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.github.markovcd.hughandkettle.amp.JavaMicrophone

/** Java's default microphone; the system asks, where it does, when the microphone is first opened. */
@Composable
internal actual fun rememberMicrophone(): MicrophoneAccess = remember { MicrophoneAccess({ it(true) }, JavaMicrophone) }
