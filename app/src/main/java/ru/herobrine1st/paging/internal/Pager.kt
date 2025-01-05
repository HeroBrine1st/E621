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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.channels.consumeEach
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
    val snapshotChannel: SendChannel<Snapshot<Key, Value>>,
    // both send and receive as we send this channel via snapshotChannel
    val requestChannel: Channel<PagingRequest<Key>>,
    var pages: List<Page<Key, Value>> = emptyList(),
    var loadStates: LoadStates = defaultLoadStates()
) {
    // Pager entry point
    // WARNING: This method is to be called ONCE PER INSTANCE. If violated, behavior is unspecified
    suspend fun startPaging(): Nothing {
        notifyObservers(UpdateKind.StateChange)
        requestChannel.consumeEach { event ->
            when (event) {
                is PagingRequest.PushPage -> push(event)
                is PagingRequest.Refresh -> refresh()
                PagingRequest.Retry -> retry()
            }
        }
        error("UI request channel is cancelled or closed, which should not happen")
    }

    // "Public" API called via requestChannel
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

    suspend fun push(event: PagingRequest.PushPage<Key>) {
        when (event) {
            is PagingRequest.AppendPage -> when {
                // Ignore repeated requests
                pages.last().nextKey != event.lastKey -> {
                    debug {
                        Log.w(
                            TAG,
                            "Got repeated append request: $event but last key is ${pages.last().nextKey}"
                        )
                    }
                    return
                }

                loadStates.append is LoadState.Error -> {
                    // It is NOT expected for UI part to retry repeatedly
                    Log.w(TAG, "Tried to append without retry: $loadStates")
                    return
                }
            }

            is PagingRequest.PrependPage -> when {
                // Ignore repeated requests
                pages.first().prevKey != event.firstKey -> {
                    debug {
                        Log.w(
                            TAG,
                            "Got repeated prepend request: $event but first key is ${pages.first().prevKey}"
                        )
                    }
                    return
                }

                loadStates.prepend is LoadState.Error -> {
                    // It is NOT expected for UI part to retry repeatedly
                    Log.w(TAG, "Tried to prepend without retry: $loadStates")
                    return
                }

            }
        }
        pushUnsafe(event.toPushType())
    }

    suspend fun retry() {
        if (loadStates.refresh is LoadState.Error) {
            refreshUnsafe()
        } else {
            // it may be both directions
            if (loadStates.append is LoadState.Error) {
                pushUnsafe(PushType.APPEND)
            }
            if (loadStates.prepend is LoadState.Error) {
                pushUnsafe(PushType.PREPEND)
            }
        }
    }

    // Private helper methods

    private suspend fun notifyObservers(updateKind: UpdateKind) {
        // WARNING: This is not the only place where snapshot is sourced
        // Other places:
        // - State preservation feature requires passing Snapshot instance down the flow synchronously
        //   ru.herobrine1st.paging.createPager
        // Remember to update accordingly
        snapshotChannel.send(
            Snapshot(
                pages = pages,
                updateKind = updateKind,
                pagingConfig = config,
                loadStates = loadStates,
                requestChannel = requestChannel
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

    private suspend fun pushUnsafe(type: PushType) {
        val key = when (type) {
            PushType.APPEND -> pages.last().nextKey
            PushType.PREPEND -> pages.first().prevKey
        } ?: run {
            Log.w(TAG, "Requested page push without key available: $type")
            return
        }

        loadStates = when (type) {
            PushType.APPEND -> loadStates.copy(append = LoadState.Loading)
            PushType.PREPEND -> loadStates.copy(prepend = LoadState.Loading)
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
                val appended: Int
                val prepended: Int
                pages = buildList(capacity = pages.size + 1) {
                    addAll(pages)
                    when (type) {
                        PushType.APPEND -> {
                            add(Page.from(result, key))
                            appended = 1
                            if (size > config.maxPagesInMemory) {
                                prepended = -1
                                // drop first page
                                removeAt(0)
                                debug {
                                    Log.d(
                                        TAG,
                                        "Dropping first page due to page count being larger than configured (${config.maxPagesInMemory}) and now it is $size"
                                    )
                                }
                            } else {
                                prepended = 0
                            }
                        }

                        PushType.PREPEND -> {
                            add(0, Page.from(result, key))
                            prepended = 1
                            if (size > config.maxPagesInMemory) {
                                appended = -1
                                // drop last page
                                removeAt(lastIndex)
                                debug {
                                    Log.d(
                                        TAG,
                                        "Dropping last page due to page count being larger than configured (${config.maxPagesInMemory}) and now it is $size"
                                    )
                                }
                            } else {
                                appended = 0
                            }
                        }
                    }
                }

                loadStates = when (type) {
                    PushType.APPEND -> loadStates.copy(
                        append = LoadState.NotLoading(result.nextKey == null),
                        prepend = if (prepended < 0) LoadState.NotLoading else loadStates.prepend
                    )

                    PushType.PREPEND -> loadStates.copy(
                        prepend = LoadState.NotLoading(result.previousKey == null),
                        append = if (appended < 0) LoadState.NotLoading else loadStates.append
                    )
                }
                updateKind = UpdateKind.DataChange(
                    appended = appended,
                    prepended = prepended
                )
            }

            is LoadResult.Error<Key, Value> -> {
                loadStates = loadStates.copy(
                    append = LoadState.Error(result.throwable.message)
                )
                updateKind = UpdateKind.StateChange
                Log.e(TAG, "PagingSource returned LoadResult.Error", result.throwable)
            }
        }

        notifyObservers(updateKind)
    }
}

private enum class PushType {
    APPEND,
    PREPEND;
}

private fun PagingRequest.PushPage<*>.toPushType() = when (this) {
    is PagingRequest.AppendPage -> PushType.APPEND
    is PagingRequest.PrependPage -> PushType.PREPEND
}

