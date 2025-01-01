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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import ru.herobrine1st.paging.api.LoadStates
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingSource
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.internal.Page
import ru.herobrine1st.paging.internal.Pager
import ru.herobrine1st.paging.internal.PagingRequest
import ru.herobrine1st.paging.internal.SynchronizedBus
import ru.herobrine1st.paging.internal.UpdateKind
import ru.herobrine1st.paging.internal.defaultLoadStates

/**
 * This function creates an instance of pager, ready to be connected to UI.
 *
 * This variant of pager does not support state preservation feature.
 *
 * @param config Pager configuration.
 * @param initialKey Key used for getting initial paging data (i.e. refreshing).
 * @param pagingSource Instance of actual content provider.
 */
fun <Key : Any, Value : Any> createPager(
    config: PagingConfig,
    initialKey: Key,
    pagingSource: PagingSource<Key, Value>
): Flow<Snapshot<Key, Value>> = channelFlow {
    Pager(
        config = config,
        initialKey = initialKey,
        pagingSource = pagingSource,
        channel = channel,
        uiChannel = SynchronizedBus<PagingRequest>()
    ).startPaging()
}

/**
 * This function creates an instance of pager, ready to be connected to UI.
 *
 * This variant of pager supports state preservation feature. Provide [initialState] to enable it.
 *
 * @param config Pager configuration.
 * @param initialKey Key used for getting initial paging data (i.e. refreshing).
 * @param pagingSource Instance of actual content provider.
 * @param initialState used to restore state previously saved with [ru.herobrine1st.paging.api.getStateForPreservation]
 */
fun <Key : Any, Value : Any> CoroutineScope.createPager(
    config: PagingConfig,
    initialKey: Key,
    pagingSource: PagingSource<Key, Value>,
    initialState: Pair<List<Page<Key, Value>>, LoadStates>?
): SharedFlow<Snapshot<Key, Value>> {
    val uiChannel = SynchronizedBus<PagingRequest>()
    val flow = channelFlow {
        Pager(
            config = config,
            initialKey = initialKey,
            pagingSource = pagingSource,
            // if this particular line is extracted from constructor and delayed, code below will be safe as we can reuse Pager code
            // to create Snapshot instance
            // But delaying channel provision creates problems on its own
            channel = channel,
            uiChannel = uiChannel,
            pages = initialState?.first ?: emptyList<Page<Key, Value>>(),
            loadStates = initialState?.second ?: defaultLoadStates()
        ).startPaging()
    }
    val sharedFlow = MutableSharedFlow<Snapshot<Key, Value>>(replay = 1)
    if (initialState != null) {
        // SAFETY: UNSAFE
        // See Pager comments on Snapshot creation (notifyObservers method)
        //
        // Synchronously create Snapshot and pass it to replayCache for PagingItemsImpl to pick it up also synchronously
        sharedFlow.tryEmit(
            Snapshot(
                pages = initialState.first,
                updateKind = UpdateKind.Refresh,
                pagingConfig = config,
                loadStates = initialState.second,
                uiChannel = uiChannel
            )
        )
    }

    // SAFETY: uiChannel reuse is prohibited by not exposing initial pager flow
    // and so this line is guaranteed to be the only one single collection of that flow.
    launch { flow.collect(sharedFlow) }

    return sharedFlow.asSharedFlow()
}

