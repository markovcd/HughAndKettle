package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.markovcd.hughandkettle.amp.PresetNameStore
import io.github.markovcd.hughandkettle.amp.SongStore
import io.github.markovcd.hughandkettle.amp.ToneStore
import java.nio.file.Path

/**
 * Where the app keeps what it saves: the folders of tones, preset names and songs under [folder], and
 * each setting as a key of its own in [settings]. The phone passes its files folder and its settings file;
 * the desktop passes `~/.hk`, which `hk` reads too, and a settings file there.
 */
class AppHome(
    folder: Path,
    settings: DataStore<Preferences>,
) {
    val tones: ToneStore = ToneStore(folder.resolve("tones"))
    val presetNames: PresetNameStore = PresetNameStore(folder.resolve("preset-names"))
    val songs: SongStore = SongStore(folder.resolve("songs"))
    val theme: ThemeStore = ThemeStore(settings)
    val amps: AmpChoiceStore = AmpChoiceStore(settings)
    val lastWmi: LastWmiStore = LastWmiStore(settings)
}
