/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2025 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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
import kotlinx.coroutines.channels.SendChannel
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.paging.api.LoadResult
import ru.herobrine1st.paging.api.LoadState
import ru.herobrine1st.paging.api.LoadState.NotLoading.invoke
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingSource
import ru.herobrine1st.paging.api.Snapshot

private const val TAG = "Pager"

fun defaultLoadStates() = LoadStates(
    prepend = LoadState.Idle,
    append = LoadState.Idle,
    refresh = LoadState.NotLoading
)

// WARNING: This class is NOT intended to be used directly
class Pager<Key : Any, Value : Any>(
    private val config: PagingConfig,
    private val initialKey: Key,
    private val pagingSource: PagingSource<Key, Value>,
    val channel: SendChannel<Snapshot<Key, Value>>,
    val uiChannel: SynchronizedBus<PagingRequest>,
    var pages: List<Page<Key, Value>> = emptyList(),
    var loadStates: LoadStates = defaultLoadStates()
) {
    // Pager entry point
    // WARNING: This method is to be called ONCE PER INSTANCE. If violated, behavior is unspecified
    suspend fun startPaging(): Nothing {
//        notifyObservers(
//            if (pages.isNotEmpty()) UpdateKind.Refresh // it's like we made a request to server and got all the data instantly
//            else UpdateKind.StateChange
//        )
        // Probably it is enough as createPager emits `Refresh` itself
        notifyObservers(UpdateKind.StateChange)
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
        // WARNING: This is not the only place where snapshot is sourced
        // Other places:
        // - State preservation feature requires passing Snapshot instance down the flow synchronously
        //   ru.herobrine1st.paging.createPager
        // Remember to update
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


