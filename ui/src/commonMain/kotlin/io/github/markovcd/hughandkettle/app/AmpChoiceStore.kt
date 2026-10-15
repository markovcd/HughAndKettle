package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.markovcd.hughandkettle.amp.AmpModel
import io.github.markovcd.hughandkettle.wmi.WmiAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/** The amp picked for each WMI-1 in the settings; a damaged settings file reads as none picked. */
class AmpChoiceStore(
    private val store: DataStore<Preferences>,
) {
    val choices: Flow<Map<WmiAddress, AmpModel>> =
        store.data
            .map { AmpChoices.read(it[KEY]) }
            .catch { emit(emptyMap()) }

    suspend fun remember(
        box: WmiAddress,
        amp: AmpModel,
    ) {
        store.edit { it[KEY] = AmpChoices.write(AmpChoices.read(it[KEY]) + (box to amp)) }
    }

    private companion object {
        // Persisted: never renamed.
        val KEY = stringPreferencesKey("amps")
    }
}
