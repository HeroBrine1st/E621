/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2024 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ru.herobrine1st.e621.preference


import android.content.Context
import androidx.compose.runtime.compositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by dataStore(
    fileName = "preferences.pb",
    serializer = PreferencesSerializer
)

// dataStore.data is not a state flow so it causes first-frame issues
val LocalPreferences = compositionLocalOf<Preferences> { error("No preferences in this scope") }

// Helper functions to avoid boilerplate

suspend inline fun Context.updatePreferences(
    crossinline block: suspend Preferences.() -> Preferences
) = dataStore.updatePreferences(block)

inline fun <T> Context.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = dataStore.getPreferencesFlow(transform)

fun Context.getPreferencesFlow() = dataStore.data

// DataStore methods

suspend inline fun DataStore<Preferences>.updatePreferences(
    crossinline block: suspend Preferences.() -> Preferences
) = updateData { it.block() }

inline fun <T> DataStore<Preferences>.getPreferencesFlow(
    crossinline transform: suspend (Preferences) -> T
): Flow<T> = data.map(transform)


