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

package ru.herobrine1st.e621.navigation.component.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.decompose.router.stack.pop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.database.entity.BlacklistEntry
import ru.herobrine1st.e621.database.repository.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.LifecycleScope

class SettingsBlacklistEntryComponent(
    componentContext: ComponentContext,
    val id: Long,
    initialQuery: String,
    private val enabled: Boolean,
    private val blacklistRepository: BlacklistRepository,
    private val navigator: StackNavigator<*>
) : ComponentContext by componentContext {

    var query by mutableStateOf(initialQuery)

    private val lifecycleScope = LifecycleScope()

    fun apply(callback: () -> Unit) {
        if (query.isEmpty()) return
        lifecycleScope.launch {
            if (id != 0L) blacklistRepository.updateEntry(
                BlacklistEntry(
                    id = id,
                    query = query,
                    enabled = enabled
                )
            )
            else blacklistRepository.insertEntry(
                BlacklistEntry(
                    query, enabled
                )
            )
            withContext(Dispatchers.Main.immediate) {
                callback()
                navigator.pop()
            }
        }
    }
}