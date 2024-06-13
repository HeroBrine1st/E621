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

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingItems
import ru.herobrine1st.paging.api.Snapshot
import kotlin.math.absoluteValue
import kotlin.math.sign

private val TAG = "PagingItemsImpl"

class PagingItemsImpl<Value : Any>(
    val flow: Flow<Snapshot<*, Value>>,
    private val startPagingImmediately: Boolean
) : PagingItems<Value> {

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

    override val size: Int
        get() = items.size
    override var items: List<Value> by mutableStateOf(emptyList())
        private set


    private var lastAccessedIndex = -1

    init {
        if (flow is SharedFlow<Snapshot<*, Value>>) {
            // Assuming it is a result of cachedIn, process cached value synchronously
            val cachedSnapshot = flow.replayCache.firstOrNull()
            if (cachedSnapshot != null) {
                processSnapshot(cachedSnapshot)
            } else if (startPagingImmediately) {
                // If there's no cached value, assume it is first usage of this particular pager
                // and update loadStates ahead of time to avoid first-frame issues
                loadStates = loadStates.copy(refresh = LoadState.Loading)
                debug {
                    Log.d(TAG, "Updating loadStates.refresh to Loading ahead of time")
                }
            }
        }
    }

    private fun processSnapshot(snapshot: Snapshot<*, Value>) {
        if (uiChannel == null) {
            debug { Log.d(TAG, "Got uiChannel and pagingConfig") }
            uiChannel = snapshot.uiChannel
            pagingConfig = snapshot.pagingConfig
        }
        if (startPagingImmediately && snapshot.loadStates.refresh is LoadState.NotLoading) {
            // SAFETY: Upstream pager state is NotLoading; refresh method does not know that
            // SAFETY: uiChannel is never null here
            uiChannel!!.send(PagingRequest.Refresh)
            debug {
                assert(
                    loadStates == LoadStates(
                        prepend = LoadState.Idle,
                        append = LoadState.Idle,
                        refresh = LoadState.Loading
                    )
                ) { "State is initialized incorrectly: got $loadStates but initial start condition is satisfied" }
                Log.d(TAG, "Sending refresh request because immediate start is requested")
            }
            return
        } else {
            // Either startPagingImmediately is false so that loadStates.refresh guarantees are satisfied
            // or snapshot.loadStates.refresh is not NotLoading so that those guarantees are again satisfied
            loadStates = snapshot.loadStates
        }

        when (val updateKind = snapshot.updateKind) {
            is UpdateKind.Refresh -> {
                lastAccessedIndex = -1
                items = snapshot.pages.flatMap { it.data }
            }

            is UpdateKind.DataChange -> {
                if (updateKind.prepended != 0) {
                    // Change lastAccessedIndex accordingly
                    // It may be unnecessary, but it is harmless, I think
                    val pages = updateKind.prepended.absoluteValue
                    val sign = updateKind.prepended.sign
                    lastAccessedIndex = sign * snapshot.pages.take(pages).sumOf { it.data.size }
                }
                items = snapshot.pages.flatMap { it.data }
                // Fix possible edge cases where user otherwise have to violently scroll to trigger load
                // Also it immediately requests new page if appended page is empty, avoiding visual bugs
                if (lastAccessedIndex != -1) triggerPageLoad()
            }

            UpdateKind.StateChange -> {}
        }
    }

    suspend fun collectPagingData() = flow.collect(::processSnapshot)

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
        return items[index]
    }

    override fun peek(index: Int): Value {
        return items[index]
    }

    override fun refresh() {
        if (loadStates.refresh == LoadState.Loading) return
        uiChannel?.send(PagingRequest.Refresh)
    }
}