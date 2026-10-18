package io.github.markovcd.hughandkettle.desktop

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.markovcd.hughandkettle.app.AppHome
import io.github.markovcd.hughandkettle.app.LinkHolder
import io.github.markovcd.hughandkettle.wmi.LinkState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.nio.file.Path
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Holds what outlives the window: the link to the WMI-1 or the cable, the cables Java sees, the click a song
 * counts in with, and what the app keeps in [folder], `~/.hk` unless a test says otherwise. The process holds
 * the link; there is no service to keep it.
 */
class DesktopApp(
    folder: Path,
    val jack: JavaCableJack = JavaCableJack(),
    lookEvery: Duration = 2.seconds,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val link: LinkHolder = LinkHolder(jack)
    val home: AppHome =
        AppHome(
            folder,
            PreferenceDataStoreFactory.create(scope = scope) { folder.resolve("settings.preferences_pb").toFile() },
        )
    val click: DesktopClickSound = DesktopClickSound()

    init {
        scope.launch {
            while (isActive) {
                jack.look()
                delay(lookEvery)
            }
        }
        scope.launch {
            link.state.filterIsInstance<LinkState.Connected>().collect { home.lastWmi.remember(it.address) }
        }
    }

    override fun close() {
        link.close()
        scope.cancel()
    }

    companion object {
        /** The folder `hk` keeps its tones, preset names and songs in, shared with the desktop app. */
        val HK_FOLDER: Path = Path.of(System.getProperty("user.home"), ".hk")
    }
}
