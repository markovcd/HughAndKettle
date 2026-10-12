package io.github.markovcd.hughandkettle.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The last WMI-1 a link reached, kept on the phone so it can be offered first next time. */
class LastWmiStore(
    context: Context,
) {
    private val store = context.applicationContext.settings

    /** The last box, or null when there is none or it cannot be read; a damaged file never stops the app. */
    val last: Flow<WmiAddress?> =
        store.data
            .map { LastWmi.read(it[KEY]) }
            .catch { emit(null) }

    suspend fun remember(address: WmiAddress) {
        store.edit { it[KEY] = LastWmi.write(address) }
    }

    private companion object {
        // Persisted: never renamed.
        val KEY = stringPreferencesKey("last_wmi")
    }
}
