package io.github.markovcd.hughandkettle.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performCustomAccessibilityActionWithLabel
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.github.markovcd.hughandkettle.amp.AmpModels
import io.github.markovcd.hughandkettle.amp.NoteLength
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.SongStep
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.Tone
import io.github.markovcd.hughandkettle.wmi.FakeWmi
import io.github.markovcd.hughandkettle.wmi.WmiAddress
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
    fun `In the Tones mode Add step adds the loaded tone, and with none loaded it is off`() =
        runComposeUiTest {
            val lead = Tone("Lead solo", panel.amp, mapOf("gain" to 90))
            var loaded by mutableStateOf<Tone?>(null)
            setContent { SongTab(songs, names, preset = 5, enabled = true, mode = SwitcherMode.Tones, loaded = loaded) }
            onNodeWithTag("song_add").assertIsNotEnabled()
            loaded = lead
            onNodeWithTag("song_add").performClick()
            assertEquals(listOf(SongStep(beats = SongState.NEW_STEP_BEATS, tone = "Lead solo")), songs.song.steps)
            onNodeWithTag("song_step_kind:1").assertExists()
        }

    @Test
    fun `A step on a deleted tone is marked and said over the steps`() =
        runComposeUiTest {
            var shelf by mutableStateOf(listOf(Tone("Lead solo", panel.amp, mapOf("gain" to 90))))
            val songs =
                SongState(
                    SongStore(folder.resolve("tone-songs")),
                    panel,
                    scope,
                    Dispatchers.Unconfined,
                    tones = { shelf },
                )
            songs.add(shelf.single())
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_missing").assertDoesNotExist()
            shelf = emptyList()
            onNodeWithTag("song_missing").assertExists()
            onNodeWithTag("song_step_kind:1").assertExists()
        }

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
    fun `A step's note is picked from its row while the tempo lock is on, and put back on the note picked`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_note:1").assertDoesNotExist()
            songs.lock(on = true)
            onNodeWithTag("song_note:1").performClick()
            onNodeWithTag("song_note_pick:1/8d").performClick()
            assertEquals(
                NoteLength.of("1/8d"),
                songs.song.steps
                    .single()
                    .note,
            )
            onNodeWithTag("song_note:1").performClick()
            onNodeWithTag("song_note_pick:none").performClick()
            assertEquals(
                null,
                songs.song.steps
                    .single()
                    .note,
            )
        }

    @Test
    fun `A step is moved up and down by its accessibility actions`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_beats_up:1").performClick()
            onNodeWithTag("song_add").performClick()
            onNodeWithTag("song_step:2").performCustomAccessibilityActionWithLabel("Move step 2 up")
            assertEquals(listOf(16, 20), beats())
            onNodeWithTag("song_step:1").performCustomAccessibilityActionWithLabel("Move step 1 down")
            assertEquals(listOf(20, 16), beats())
        }

    @Test
    fun `A step held and dragged past the one above trades places with it`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            repeat(3) { onNodeWithTag("song_add").performClick() }
            onNodeWithTag("song_beats_up:3").performClick()
            onNodeWithTag("song_step:3").performTouchInput {
                down(center)
                advanceEventTime(viewConfiguration.longPressTimeoutMillis + 100)
                repeat(5) { moveBy(Offset(0f, -height * 0.2f)) }
                up()
            }
            assertEquals(listOf(16, 20, 16), beats())
        }

    @Test
    fun `The song list opens from the song's name, and a song tapped in it opens and closes the list`() =
        runComposeUiTest {
            songs.create("Highway Star")
            songs.create("Slow blues")
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_list").performClick()
            onNodeWithTag("song_open:Slow blues").assertIsSelected()
            onNodeWithTag("song_open:Highway Star").performClick()
            assertEquals("Highway Star", songs.song.name)
            onNodeWithTag("song_list_rows").assertDoesNotExist()
        }

    @Test
    fun `A new song is named in its dialog, which refuses a name another song has`() =
        runComposeUiTest {
            songs.create("Encore")
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_list").performClick()
            onNodeWithTag("song_new").performClick()
            onNodeWithTag("song_name").performTextReplacement("encore")
            onNodeWithTag("song_new_create").assertIsNotEnabled()
            onNodeWithTag("song_name").performTextReplacement("Set opener")
            onNodeWithTag("song_new_create").performClick()
            assertEquals("Set opener", songs.song.name)
            assertEquals(listOf("Encore", "Set opener"), songs.songs.map { it.name })
        }

    @Test
    fun `A song is deleted from its menu in the list once the delete is confirmed`() =
        runComposeUiTest {
            songs.create("Encore")
            songs.create("Set opener")
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_list").performClick()
            onNodeWithTag("song_menu:Encore").performClick()
            onNodeWithTag("song_delete:Encore").performClick()
            onNodeWithTag("song_delete_cancel").performClick()
            assertEquals(2, songs.songs.size)
            onNodeWithTag("song_menu:Encore").performClick()
            onNodeWithTag("song_delete:Encore").performClick()
            onNodeWithTag("song_delete_confirm").performClick()
            assertEquals(listOf("Set opener"), songs.songs.map { it.name })
        }

    @Test
    fun `Record is off and says why while the amp is not linked`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = false) }
            onNodeWithTag("song_record").assertIsNotEnabled()
            onNodeWithTag("song_record_offline").assertExists()
        }

    @Test
    fun `The click while recording is switched beside the count-in`() =
        runComposeUiTest {
            setContent { SongTab(songs, names, preset = 5, enabled = true) }
            onNodeWithTag("song_record_click").performClick()
            assertEquals(true, songs.clicking)
        }

    @Test
    fun `Record shows the take as it grows, and Stop keeps it with Discard offered`() =
        runComposeUiTest {
            val fake = FakeWmi()
            try {
                panel.connect(WmiAddress("127.0.0.1", fake.port))
                waitUntil("linked", timeoutMillis = 5_000) { panel.linked }
                songs.countIn(false)
                panel.recall(5)
                setContent { SongTab(songs, names, preset = 5, enabled = true) }
                onNodeWithTag("song_record").performClick()
                onNodeWithTag("song_recording").assertExists()
                onNodeWithTag("song_step:1").assertExists()
                onNodeWithTag("song_record_stop").performClick()
                onNodeWithTag("song_take_notice").assertExists()
                assertEquals("Take 1", songs.song.name)
                onNodeWithTag("song_take_discard").performClick()
                onNodeWithTag("song_take_notice").assertDoesNotExist()
            } finally {
                fake.close()
            }
        }
}
