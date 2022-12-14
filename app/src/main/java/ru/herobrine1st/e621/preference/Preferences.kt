package ru.herobrine1st.e621.preference


import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences

val Context.dataStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer
)

// dataStore.data is not a state flow so it causes first-frame issues
val LocalPreferences = compositionLocalOf<Preferences> { error("No preferences in this scope") }

// Helper functions to avoid boilerplate

suspend inline fun Context.updatePreferences(
    crossinline block: suspend Preferences.Builder.() -> Unit
) = dataStore.updatePreferences(block)

inline fun <T> Context.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = dataStore.getPreferencesFlow(transform)

fun Context.getPreferencesFlow() = dataStore.data

// DataStore methods

suspend inline fun DataStore<Preferences>.updatePreferences(
    crossinline block: suspend Preferences.Builder.() -> Unit
) = updateData { it.toBuilder().apply { block() }.build() }

inline fun <T> DataStore<Preferences>.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = data.map(transform)


