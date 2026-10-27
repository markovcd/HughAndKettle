package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** Whether a take clicks every beat, kept in the settings; a damaged settings file reads as off. */
class RecordClickStore(
    private val store: DataStore<Preferences>,
) {
    val on: Flow<Boolean> =
        store.data
            .map { it[KEY] ?: false }
            .catch { emit(false) }

    suspend fun keep(on: Boolean) {
        store.edit { it[KEY] = on }
    }

    private companion object {
        // Persisted: never renamed.
        val KEY = booleanPreferencesKey("record_click")
    }
}
