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

package ru.herobrine1st.e621.navigation.component.post

import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.PoolId
import ru.herobrine1st.e621.navigation.LifecycleScope

class PoolsDialogComponent(
    componentContext: ComponentContext,
    api: API,
    pools: List<PoolId>,
    private val openPool: (Pool) -> Unit,
    private val close: () -> Unit,
) : ComponentContext by componentContext {
    val lifecycleScope = LifecycleScope()

    private val _pools = pools.map { PoolState.NotLoaded }.toMutableStateList<PoolState>()

    val pools: List<PoolState> by ::_pools

    init {
        lifecycle.doOnResume {
            lifecycleScope.launch {
                pools.indices.forEach { index ->
                    if (_pools[index] == PoolState.NotLoaded) {
                        _pools[index] = api.getPool(pools[index]).map {
                            PoolState.Successful(it)
                        }.recover {
                            PoolState.Error
                        }.getOrThrow()
                    }
                }
            }
        }
    }

    fun onClick(pool: Pool) = openPool(pool)

    fun onDismiss() = close()

    sealed interface PoolState {
        data class Successful(val pool: Pool) : PoolState

        data object Error : PoolState

        data object NotLoaded : PoolState
    }
}