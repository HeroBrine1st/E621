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

package ru.herobrine1st.paging.internal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingItems
import ru.herobrine1st.paging.api.Snapshot
import kotlin.math.absoluteValue
import kotlin.math.sign

class PagingItemsImpl<Key : Any, Value : Any>(
    val flow: Flow<Snapshot<Key, Value>>,
) : PagingItems<Key, Value> {

    private var listDelegate by mutableStateOf(emptyList<Value>())
    private var uiChannel: SynchronizedBus<PagingRequest>? = null
    private var pagingConfig: PagingConfig? = null

    override var loadStates by mutableStateOf(
        LoadStates(
            LoadState.Idle,
            LoadState.Idle,
            LoadState.Idle
        )
    )
        private set

    suspend fun collectPagingData(startImmediately: Boolean) {
        flow.collect { snapshot ->
            loadStates = snapshot.loadStates
            if (uiChannel == null) {
                uiChannel = snapshot.uiChannel
                pagingConfig = snapshot.pagingConfig
                if (startImmediately && loadStates.refresh is LoadState.NotLoading) refresh()
            }

            when (val updateKind = snapshot.updateKind) {
                is UpdateKind.Refresh -> {
                    lastAccessedIndex = -1
                    listDelegate = snapshot.pages.flatMap { it.data }
                }

                is UpdateKind.DataChange -> {
                    if (updateKind.prepended != 0) {
                        // Change lastAccessedIndex accordingly
                        // It can be unnecessary, but it is harmless, I think
                        val pages = updateKind.prepended.absoluteValue
                        val sign = updateKind.prepended.sign
                        lastAccessedIndex = sign * snapshot.pages.take(pages).sumOf { it.data.size }
                    }
                    listDelegate = snapshot.pages.flatMap { it.data }
                    // Fix possible edge cases where user otherwise have to violently scroll to trigger load
                    // Also it immediately requests new page if appended page is empty, avoiding visual bugs
                    if (lastAccessedIndex != -1) triggerPageLoad()
                }

                UpdateKind.StateChange -> {}
            }
        }
    }

    override val size: Int
        get() = listDelegate.size
    override val items: List<Value>
        get() = listDelegate

    private var lastAccessedIndex = -1

    private fun triggerPageLoad() {
        val prefetchDistance = pagingConfig?.prefetchDistance ?: 1
        if (lastAccessedIndex < prefetchDistance && loadStates.prepend == LoadState.NotLoading) {
            uiChannel?.send(PagingRequest.Prepend)
        } else if (lastAccessedIndex >= size - prefetchDistance && loadStates.append == LoadState.NotLoading) {
            uiChannel?.send(PagingRequest.Append)
        }
    }

    override fun get(index: Int): Value {
        lastAccessedIndex = index
        triggerPageLoad()
        return listDelegate[index]
    }

    override fun peek(index: Int): Value {
        return listDelegate[index]
    }

    override fun refresh() {
        if (loadStates.refresh == LoadState.Loading) return
        uiChannel?.send(PagingRequest.Refresh)
    }
}