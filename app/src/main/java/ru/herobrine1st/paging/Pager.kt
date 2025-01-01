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

package ru.herobrine1st.paging

import android.util.Log
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.paging.api.LoadResult
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingSource
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.internal.LoadParamsImpl
import ru.herobrine1st.paging.internal.Page
import ru.herobrine1st.paging.internal.PagingRequest
import ru.herobrine1st.paging.internal.SynchronizedBus
import ru.herobrine1st.paging.internal.UpdateKind

private const val TAG = "Pager"

class Pager<Key : Any, Value : Any>(
    private val config: PagingConfig,
    private val initialKey: Key,
    private val pagingSource: PagingSource<Key, Value>,
    /**
     * This parameter is used to restore state previously saved with [ru.herobrine1st.paging.api.getStateForPreservation]
     */
    private var initialState: Pair<List<Page<Key, Value>>, LoadStates>? = null
) {
    // TODO thread unsafety is largely introduced not by mutable data but by class used as a mere function, which encourages instance reuse
    //      Replace this class with function returning flow, and rename PagerState to Pager as it is actual Pager
    //      If this class were a function, initialState could be a simple non-mutable parameter with side-effect of restoring state on each collection
    //      which can be mitigated by explicitly saying that multi-collection leads to undefined behavior and recommendation to use cachedIn
    // side-note: we can create function returning Flow, preserve cachedIn function and also create function returning StateFlow
    // that combines state restoration and custom implementation of cachedIn to incorporate synchronous state restoration

    val flow: Flow<Snapshot<Key, Value>> = channelFlow {
        val state = initialState?.let { state ->
            // SAFETY: in worst case scenario, concurrent usage can create multiple Flows with the same initial state
            //         but PagerState is copy-on-write, so no harm is possible
            // The main goal is not harmed either: for initial state reuse there needs a concurrent usage while this
            // code ensures that state can't go back in time e.g. when this flow is collected second time,
            // and concurrent usage is in the same point of time - so time travel is impossible.
            // Also this is intended to be used with cachedIn, which ensures singular collection.
            initialState = null
            PagerState(
                channel,
                pages = state.first,
                loadStates = state.second
            )
        } ?: PagerState(channel)

        state.startPaging()
    }

    private inner class PagerState(
        val channel: SendChannel<Snapshot<Key, Value>>,
        var pages: List<Page<Key, Value>> = emptyList(),
        var loadStates: LoadStates = LoadStates(
            prepend = LoadState.Idle,
            append = LoadState.Idle,
            refresh = LoadState.NotLoading
        )
    ) {
        val uiChannel: SynchronizedBus<PagingRequest> = SynchronizedBus<PagingRequest>()

        // Pager entry point
        // WARNING: This method is to be called ONCE PER INSTANCE. If violated, behavior is unspecified
        suspend fun startPaging(): Nothing {
            notifyObservers(
                if (pages.isNotEmpty()) UpdateKind.Refresh // it's like we made a request to server and got all the data synchronously
                else UpdateKind.StateChange
            )
            uiChannel.flow.collect { event ->
                when (event) {
                    is PagingRequest.PushPage -> push(event)
                    is PagingRequest.Refresh -> refresh()
                    PagingRequest.Retry -> retry()
                }
            }
            error("UI event channel collection is complete, which should not happen")
        }

        // "Public" API called via uiChannel
        suspend fun refresh() {
            if (loadStates.refresh is LoadState.Error) {
                // It is NOT expected for UI part to retry repeatedly
                Log.w(TAG, "Tried to refresh without retry: $loadStates")
                debug {
                    // fail-fast
                    Log.wtf(TAG, "Tried to refresh without retry: $loadStates")
                }
                return
            }
            refreshUnsafe()
        }

        suspend fun push(event: PagingRequest.PushPage) {
            when (event) {
                PagingRequest.AppendPage -> if (loadStates.append is LoadState.Error) {
                    // It is NOT expected for UI part to retry repeatedly
                    Log.w(TAG, "Tried to append without retry: $loadStates")
                    debug {
                        // fail-fast
                        Log.wtf(TAG, "Tried to append without retry: $loadStates")
                    }
                    return
                }

                PagingRequest.PrependPage -> if (loadStates.prepend is LoadState.Error) {
                    // It is NOT expected for UI part to retry repeatedly
                    Log.w(TAG, "Tried to prepend without retry: $loadStates")
                    debug {
                        // fail-fast
                        Log.wtf(TAG, "Tried to prepend without retry: $loadStates")
                    }
                    return
                }
            }
            pushUnsafe(event)
        }

        suspend fun retry() {
            if (loadStates.refresh is LoadState.Error) {
                refreshUnsafe()
            } else {
                // it may be both directions
                if (loadStates.append is LoadState.Error) {
                    pushUnsafe(PagingRequest.AppendPage)
                }
                if (loadStates.prepend is LoadState.Error) {
                    pushUnsafe(PagingRequest.PrependPage)
                }
            }
        }

        // Private helper methods

        private suspend fun notifyObservers(updateKind: UpdateKind) {
            channel.send(
                Snapshot(
                    pages = pages,
                    updateKind = updateKind,
                    pagingConfig = config,
                    loadStates = loadStates,
                    uiChannel = uiChannel
                )
            )
        }

        // Unsafety means it is not safe to automatically call those methods without error check,
        // as otherwise pager may repeatedly error on the same endpoint
        // Otherwise they are safe

        private suspend fun refreshUnsafe() {
            // Reset state
            loadStates = LoadStates(
                prepend = LoadState.Idle,
                append = LoadState.Idle,
                refresh = LoadState.Loading
            )

            // do not clear pages (should probably be configurable)

            notifyObservers(UpdateKind.StateChange)

            val result = pagingSource.getPage(
                LoadParamsImpl(
                    key = initialKey,
                    requestedSize = config.initialLoadSize
                )
            )

            // Apply result
            val updateKind: UpdateKind
            when (result) {
                is LoadResult.Error -> {
                    loadStates = loadStates.copy(
                        prepend = LoadState.Idle,
                        append = LoadState.Idle,
                        refresh = LoadState.Error(result.throwable.message)
                    )
                    updateKind = UpdateKind.StateChange
                }

                is LoadResult.Page -> {
                    pages = listOf(Page.from(result, initialKey))
                    loadStates = loadStates.copy(
                        prepend = LoadState.NotLoading(result.previousKey == null),
                        append = LoadState.NotLoading(result.nextKey == null),
                        refresh = LoadState.Complete
                    )
                    updateKind = UpdateKind.Refresh
                }
            }

            notifyObservers(updateKind)
        }

        private suspend fun pushUnsafe(event: PagingRequest.PushPage) {
            val key = when (event) {
                is PagingRequest.AppendPage -> pages.last().nextKey
                is PagingRequest.PrependPage -> pages.first().prevKey
            } ?: run {
                Log.w(TAG, "Requested page push without key available: $event")
                return
            }

            loadStates = when (event) {
                is PagingRequest.AppendPage -> loadStates.copy(append = LoadState.Loading)
                is PagingRequest.PrependPage -> loadStates.copy(prepend = LoadState.Loading)
            }

            notifyObservers(UpdateKind.StateChange)

            val result = pagingSource.getPage(
                LoadParamsImpl(
                    key = key,
                    requestedSize = config.pageSize
                )
            )

            val updateKind: UpdateKind

            // Apply result
            when (result) {
                is LoadResult.Page<Key, Value> -> {
                    pages = buildList(capacity = pages.size + 1) {
                        addAll(pages)
                        when (event) {
                            PagingRequest.AppendPage -> add(Page.from(result, key))
                            PagingRequest.PrependPage -> add(0, Page.from(result, key))
                        }
                    }

                    loadStates = when (event) {
                        is PagingRequest.AppendPage -> loadStates.copy(
                            append = LoadState.NotLoading(result.nextKey == null)
                        )

                        is PagingRequest.PrependPage -> loadStates.copy(
                            prepend = LoadState.NotLoading(result.previousKey == null)
                        )
                    }

                    updateKind = when (event) {
                        PagingRequest.AppendPage -> UpdateKind.DataChange(
                            appended = 1,
                            prepended = 0
                        )

                        PagingRequest.PrependPage -> UpdateKind.DataChange(
                            appended = 0,
                            prepended = 1
                        )
                    }
                }

                is LoadResult.Error<Key, Value> -> {
                    loadStates = loadStates.copy(
                        append = LoadState.Error(result.throwable.message)
                    )
                    updateKind = UpdateKind.StateChange
                }
            }

            notifyObservers(updateKind)
        }
    }
}

