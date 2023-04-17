/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.navigation.component

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.entity.BlacklistEntry
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.updatePreferences
import ru.herobrine1st.e621.util.InstanceBase

class BlacklistTogglesDialogComponent(
    onClose: () -> Unit,
    blacklistRepository: BlacklistRepository,
    applicationContext: Context,
    componentContext: ComponentContext
) : ComponentContext by componentContext {

    private val dataStore = applicationContext.dataStore
    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())
    private val instance = instanceKeeper.getOrCreate { Instance(blacklistRepository) }
    private val _onClose = onClose

    init {
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
        }
    }

    val entriesFlow: StateFlow<List<ToggleableBlacklistEntry>?>
        get() = instance.entriesFlow

    fun applyChanges(callback: () -> Unit) = instance.applyChanges(callback)


    fun toggleBlacklist(enabled: Boolean) {
        lifecycleScope.launch {
            dataStore.updatePreferences {
                blacklistEnabled = enabled
            }
        }
    }

    fun onClose() = _onClose()

    // To avoid merging states from StateKeeper and from SQLite
    private class Instance(private val blacklistRepository: BlacklistRepository) : InstanceBase() {
        val entriesFlow = blacklistRepository.getEntriesFlow()
            .map { list -> list.map { it.asToggleable() } }
            .stateIn(
                lifecycleScope,
                SharingStarted.Eagerly,
                null
            )

        fun applyChanges(callback: () -> Unit) {
            lifecycleScope.launch {
                val entries = entriesFlow.value ?: kotlin.run {
                    Log.e(
                        TAG,
                        "Illegal call to applyChanges(): data isn't even downloaded from database"
                    )
                    throw IllegalStateException()
                }
                entries.filter { it.isChanged }.map { it.toEntry() }.let {
                    blacklistRepository.updateEntries(it)
                }
                callback()
            }
        }
    }

    companion object {
        const val TAG = "BlacklistTogglesDialogViewModel"
    }
}

@Stable
class ToggleableBlacklistEntry private constructor(
    val query: String,
    private val dbEnabled: Boolean,
    val id: Long
) {
    companion object {
        fun of(blacklistEntry: BlacklistEntry): ToggleableBlacklistEntry =
            ToggleableBlacklistEntry(
                blacklistEntry.query,
                blacklistEntry.enabled,
                blacklistEntry.id
            )
    }

    var enabled by mutableStateOf(dbEnabled)

    val isChanged get() = enabled != dbEnabled


    fun resetChanges() {
        if (!isChanged) return
        enabled = dbEnabled
    }

    fun toEntry() = BlacklistEntry(query, enabled, id)
}

fun BlacklistEntry.asToggleable() = ToggleableBlacklistEntry.of(this)