package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** What the header's − and + step through, kept in the settings; a damaged settings file reads as presets. */
class SwitcherModeStore(
    private val store: DataStore<Preferences>,
) {
    val mode: Flow<SwitcherMode> =
        store.data
            .map { SwitcherMode.read(it[KEY]) }
            .catch { emit(SwitcherMode.Presets) }

    suspend fun pick(mode: SwitcherMode) {
        store.edit { it[KEY] = mode.write() }
    }

    private companion object {
        // Persisted: never renamed.
        val KEY = stringPreferencesKey("switcher")
    }
}
