package io.github.markovcd.hughandkettle.app

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** The app's one settings file on the phone; every store that keeps a setting writes a key of its own here. */
internal val Context.settings: DataStore<Preferences> by preferencesDataStore(name = "settings")
