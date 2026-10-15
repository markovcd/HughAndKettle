package io.github.markovcd.hughandkettle.app

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SongTabTest {
    private val holder = LinkHolder()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val folder: Path = Files.createTempDirectory("song-tab")
    private val panel = PanelState(AmpModels.grandMeisterDeluxe40, holder, scope, flowOf(null))
    private val names = PresetNamesState(PresetNameStore(folder.resolve("names")), panel, scope, Dispatchers.Unconfined)
    private val songs = SongState(SongStore(folder.resolve("songs")), panel, scope, Dispatchers.Unconfined)

    @AfterTest
    @OptIn(ExperimentalPathApi::class)
    fun teardown() {
        scope.cancel()
        holder.close()
        folder.deleteRecursively()
    }

    private fun beats() = songs.song.steps.map(SongStep::beats)

    @Test
    fun `A step's beats can be typed, and the buttons step them to the ends of bars`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_beats:1").performClick()
            onNodeWithTag("song_beats_field").performTextReplacement("7")
            onNodeWithTag("song_beats_done").performClick()
            assertEquals(listOf(7), beats())
            onNodeWithTag("song_beats_up:1").performClick()
            assertEquals(listOf(8), beats())
            onNodeWithTag("song_beats_down:1").performClick()
            assertEquals(listOf(4), beats())
        }

    @Test
    fun `A step is moved with its arrows`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_beats_up:1").performClick()
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_move_up:2").performClick()
            assertEquals(listOf(16, 20), beats())
            onNodeWithTag("song_move_down:1").performClick()
            assertEquals(listOf(20, 16), beats())
        }
}
