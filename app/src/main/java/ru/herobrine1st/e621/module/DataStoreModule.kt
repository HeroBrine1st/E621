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

package ru.herobrine1st.e621.module

import android.content.Context
import androidx.datastore.core.DataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import ru.herobrine1st.e621.preference.Preferences
import ru.herobrine1st.e621.preference.dataStore

typealias PreferencesStore = DataStore<Preferences>

class DataStoreModule(context: Context) {
    // No need to cancel: android.app.Application is a singleton
    private val coroutineScope = CoroutineScope(Dispatchers.Main.immediate)

    // context.applicationContext can't be used concurrently with initialization of context (android.app.Application in this case), so lazy
    @Suppress("DEPRECATION")
    val dataStore: PreferencesStore by lazy { context.dataStore }

    @CachedDataStore
    val cachedData by lazy {
        dataStore.data.stateIn(
            coroutineScope,
            SharingStarted.Eagerly,
            Preferences()
        )
    }


    // To avoid typing "dataStore" twice
    inline val data get() = dataStore.data
    suspend inline fun updateData(noinline transform: Preferences.() -> Preferences) =
        dataStore.updateData(transform)
}

@RequiresOptIn("This API is intended for usage in UI-related (i.e. synchronous) code, and improper usage can cause ACID violations")
@Retention(AnnotationRetention.BINARY)
annotation class CachedDataStore