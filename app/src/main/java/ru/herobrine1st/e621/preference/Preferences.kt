package ru.herobrine1st.e621.preference

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

fun <R> Context.getPreferenceFlow(key: Preferences.Key<R>, defaultValue: R): Flow<R> =
    this.dataStore.data.map { it[key] ?: defaultValue }

@Composable
fun <R> Context.getPreference(key: Preferences.Key<R>, defaultValue: R): R =
    getPreferenceFlow(key, defaultValue).collectAsState(initial = defaultValue).value

suspend inline fun <R> Context.setPreference(key: Preferences.Key<R>, value: R) {
    this.dataStore.edit { it[key] = value }
}

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

val BLACKLIST_ENABLED = booleanPreferencesKey("BLACKLIST_ENABLED")
val PRIVACY_MODE = booleanPreferencesKey("PRIVACY_MODE")
val PRIVACY_MODE_DIALOG_SHOWN = booleanPreferencesKey("PRIVACY_MODE_DIALOG_SHOWN")
val SHOW_REMAINING_TIME_MEDIA = booleanPreferencesKey("SHOW_REMAINING_TIME_MEDIA")
val MUTE_SOUND_MEDIA = booleanPreferencesKey("MUTE_SOUND_MEDIA")