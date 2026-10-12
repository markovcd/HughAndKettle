package io.github.markovcd.hughandkettle.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The theme the person picked, kept on the phone; a damaged settings file reads as dark. */
class ThemeStore(
    context: Context,
) {
    private val store = context.applicationContext.settings

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
