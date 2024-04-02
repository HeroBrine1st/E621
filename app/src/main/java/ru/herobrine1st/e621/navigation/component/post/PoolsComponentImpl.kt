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

package ru.herobrine1st.e621.navigation.component.post

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.ObserveLifecycleMode
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.observe
import com.arkivanov.essenty.lifecycle.subscribe
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.component.post.PoolsComponent.PoolState

private const val TAG = "PoolsComponentImpl"

class PoolsComponentImpl(
    postState: Value<PostState>,
    private val api: API,
    private val openPool: (Pool) -> Unit,
    private val onDismissRequest: () -> Unit,
    componentContext: ComponentContext,
) : PoolsComponent, ComponentContext by componentContext {
    val lifecycleScope = LifecycleScope()

    // TODO save state
    override val pools = mutableStateListOf<PoolState>()

    override var showPools by mutableStateOf(false)
        private set
    override var loadingPools by mutableStateOf(false)
        private set

    override fun openPool(pool: Pool) {
        openPool.invoke(pool)
    }

    override fun dismiss() {
        onDismissRequest()
    }

    private var downloadJob: Job? = null

    init {
        postState.observe(lifecycle, ObserveLifecycleMode.START_STOP) { state ->
            if (state !is PostState.Ready) return@observe
            if (pools.map { it.id } == state.post.pools) return@observe

            val loadedPoolsCache = pools.toList()
                .filterIsInstance<PoolState.Successful>()
                .associateBy { it.id }

            val newPools = state.post.pools.map {
                loadedPoolsCache[it] ?: PoolState.NotLoaded(it)
            }

            pools.clear()
            pools.addAll(newPools)
        }

        lifecycle.subscribe(
            onResume = {
                if (postState.value !is PostState.Ready) Log.wtf(
                    TAG,
                    "UB: Component is resumed before post information is available"
                )
                if (pools.isEmpty()) Log.wtf(TAG, "Component is resumed with no post pools")
                showPools = pools.size > 1
                downloadPools()
            },
            onPause = {
                showPools = false
                downloadJob?.cancel()
            }
        )
    }

    private fun downloadPools() {
        if (pools.none { it is PoolState.NotLoaded }) return
        downloadJob?.cancel()
        downloadJob = lifecycleScope.launch {
            loadingPools = true
            pools.forEachIndexed { index, poolState ->
                if (poolState is PoolState.NotLoaded) {
                    pools[index] = api.getPool(poolState.id).map {
                        PoolState.Successful(it)
                    }.recover {
                        PoolState.Error(poolState.id)
                    }.getOrThrow()
                }
            }
            loadingPools = false
            downloadJob = null
        }
    }
}