package ru.herobrine1st.e621.preference


import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import ru.herobrine1st.e621.preference.proto.Preferences

val Context.dataStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer
)

// Helper functions to avoid boilerplate

suspend inline fun Context.updatePreferences(
    crossinline block: suspend Preferences.Builder.() -> Preferences.Builder
) = dataStore.updateData { it.toBuilder().block().build() }

@Composable
fun Context.getPreferencesAsState() = dataStore.data
    .collectAsState(initial = PreferencesSerializer.defaultValue)

inline fun <T> Context.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = dataStore.data.map(transform)

fun Context.getPreferencesFlow() = dataStore.data

