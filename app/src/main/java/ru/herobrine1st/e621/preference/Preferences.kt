package ru.herobrine1st.e621.preference


import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.preference.proto.PreferencesOuterClass.Preferences

val Context.dataStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer
)

// Helper functions to avoid boilerplate

suspend inline fun Context.updatePreferences(
    crossinline block: suspend Preferences.Builder.() -> Unit
) = dataStore.updatePreferences(block)

inline fun <T> Context.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = dataStore.getPreferencesFlow(transform)

fun Context.getPreferencesFlow() = dataStore.data

@Composable
fun Context.getPreferencesAsState() = dataStore.data
    .collectAsState(initial = PreferencesSerializer.defaultValue)

@Composable
fun <T> Context.getPreferencesAsState(transform: (Preferences) -> T) =
    remember(transform) { dataStore.data.map(transform) }
        .collectAsState(initial = transform(PreferencesSerializer.defaultValue))


// DataStore methods

suspend inline fun DataStore<Preferences>.updatePreferences(
    crossinline block: suspend Preferences.Builder.() -> Unit
) = updateData { it.toBuilder().apply { block() }.build() }

inline fun <T> DataStore<Preferences>.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = data.map(transform)


