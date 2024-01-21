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

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
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

class Pager<Key : Any, Value : Any>(
    private val config: PagingConfig,
    private val initialKey: Key,
    private val pagingSource: PagingSource<Key, Value>,
) {
    val flow: Flow<Snapshot<Key, Value>> = channelFlow {
        val uiChannel = SynchronizedBus<PagingRequest>()

        var pages = emptyList<Page<Key, Value>>()
        var loadStates = LoadStates(
            prepend = LoadState.Idle,
            append = LoadState.Idle,
            refresh = LoadState.NotLoading
        )
        var currentGeneration = Int.MIN_VALUE

        send(
            Snapshot(
                pages,
                UpdateKind.StateChange,
                config,
                loadStates,
                uiChannel,
                currentGeneration
            )
        )

        uiChannel.flow.collect { event ->
            when (event) {
                is PagingRequest.AppendOrPrepend -> {
                    // TODO if LoadState is error for requested direction, do not retry implicitly
                    //      also add retry request
                    val key = when (event) {
                        is PagingRequest.Append -> pages.last().nextKey
                        is PagingRequest.Prepend -> pages.first().prevKey
                    } ?: return@collect

                    loadStates = when (event) {
                        is PagingRequest.Append -> loadStates.copy(append = LoadState.Loading)
                        is PagingRequest.Prepend -> loadStates.copy(prepend = LoadState.Loading)
                    }

                    currentGeneration++
                    send(
                        Snapshot(
                            pages,
                            updateKind = UpdateKind.StateChange,
                            pagingConfig = config,
                            loadStates = loadStates,
                            uiChannel = uiChannel,
                            generation = currentGeneration
                        )
                    )

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
                                    PagingRequest.Append -> add(Page.from(result, key))
                                    PagingRequest.Prepend -> add(0, Page.from(result, key))
                                }
                            }

                            loadStates = when (event) {
                                is PagingRequest.Append -> loadStates.copy(
                                    append = LoadState.NotLoading(result.nextKey == null)
                                )

                                is PagingRequest.Prepend -> loadStates.copy(
                                    prepend = LoadState.NotLoading(result.previousKey == null)
                                )
                            }

                            updateKind = when (event) {
                                PagingRequest.Append -> UpdateKind.DataChange(
                                    appended = 1,
                                    prepended = 0
                                )

                                PagingRequest.Prepend -> UpdateKind.DataChange(
                                    appended = 0,
                                    prepended = 1
                                )
                            }
                        }

                        is LoadResult.Error<Key, Value> -> {
                            loadStates = loadStates.copy(
                                append = LoadState.Error(result.throwable)
                            )
                            updateKind = UpdateKind.StateChange
                        }
                    }
                    // Notify observers
                    currentGeneration++
                    send(
                        Snapshot(
                            pages = pages,
                            updateKind = updateKind,
                            pagingConfig = config,
                            loadStates = loadStates,
                            uiChannel = uiChannel,
                            generation = currentGeneration
                        )
                    )
                }

                is PagingRequest.Refresh -> {
                    val key = initialKey

                    // Reset state
                    loadStates = LoadStates(
                        prepend = LoadState.Idle,
                        append = LoadState.Idle,
                        refresh = LoadState.Loading
                    )

                    // Notify observers
                    currentGeneration++
                    send(
                        Snapshot(
                            pages = pages, // do not clear pages (should probably be configurable)
                            updateKind = UpdateKind.StateChange,
                            pagingConfig = config,
                            loadStates = loadStates,
                            uiChannel = uiChannel,
                            generation = currentGeneration
                        )
                    )

                    val result = pagingSource.getPage(
                        LoadParamsImpl(
                            key = key,
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
                                refresh = LoadState.Error(result.throwable)
                            )
                            updateKind = UpdateKind.StateChange
                        }

                        is LoadResult.Page -> {
                            pages = listOf(Page.from(result, key))
                            loadStates = loadStates.copy(
                                prepend = LoadState.NotLoading(result.previousKey == null),
                                append = LoadState.NotLoading(result.nextKey == null),
                                refresh = LoadState.Complete
                            )
                            updateKind = UpdateKind.Refresh
                        }
                    }

                    // Notify observers
                    currentGeneration++
                    send(
                        Snapshot(
                            pages = pages,
                            updateKind = updateKind,
                            pagingConfig = config,
                            loadStates = loadStates,
                            uiChannel = uiChannel,
                            generation = currentGeneration
                        )
                    )
                }
            }
        }
    }
}

