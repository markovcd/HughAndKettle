package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The theme the person picked, kept in the settings; a damaged settings file reads as dark. */
class ThemeStore(
    private val store: DataStore<Preferences>,
) {
    val choice: Flow<ThemeChoice> =
        store.data
            .map { ThemeChoice.read(it[KEY]) }
            .catch { emit(ThemeChoice.Dark) }

    suspend fun pick(choice: ThemeChoice) {
        store.edit { it[KEY] = choice.write() }
    }

    private companion object {
        // Persisted: never renamed.
        val KEY = stringPreferencesKey("theme")
    }
}
