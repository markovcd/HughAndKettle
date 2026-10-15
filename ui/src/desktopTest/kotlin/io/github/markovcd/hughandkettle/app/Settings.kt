package io.github.markovcd.hughandkettle.app

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import java.nio.file.Path

/** A settings file in [folder], kept by [scope]; a test cancels the scope when it is done. */
internal fun settingsIn(
    folder: Path,
    scope: CoroutineScope,
): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(scope = scope) { folder.resolve("settings.preferences_pb").toFile() }
