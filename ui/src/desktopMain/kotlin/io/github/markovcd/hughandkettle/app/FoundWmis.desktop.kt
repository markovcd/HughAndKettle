package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.Composable

/** A desktop hears broadcasts with no lock to hold. */
@Composable
internal actual fun rememberBroadcastHold(): () -> AutoCloseable = { AutoCloseable {} }

/** A desktop's network is the operating system's to show. */
@Composable
internal actual fun WifiLine(discovery: DiscoveryState) = Unit
