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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.PagingSource
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.api.sanitize
import ru.herobrine1st.paging.internal.Page
import ru.herobrine1st.paging.internal.Pager
import ru.herobrine1st.paging.internal.PagingRequest
import ru.herobrine1st.paging.internal.SavedPagerState
import ru.herobrine1st.paging.internal.UpdateKind
import ru.herobrine1st.paging.internal.defaultLoadStates

private const val TAG = "CreatePager"

/**
 * This function creates an instance of pager, ready to be connected to UI.
 *
 * This variant of pager does not support state preservation feature and is not cached by default.
 * Use [ru.herobrine1st.paging.api.cachedIn] to persist this pager across collections.
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
    val requestChannel = Channel<PagingRequest<Key>>(capacity = Channel.CONFLATED)
    invokeOnClose {
        requestChannel.cancel()
    }
    Pager(
        config = config,
        initialKey = initialKey,
        pagingSource = pagingSource,
        snapshotChannel = channel,
        requestChannel = requestChannel
    ).startPaging()
}

/**
 * This function creates an instance of pager, ready to be connected to UI.
 *
 * This variant of pager supports state preservation feature. Provide [initialState] to enable it.
 * This pager is cached by default.
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
    initialState: SavedPagerState<Key, Value>?
): SharedFlow<Snapshot<Key, Value>> {
    debug {
        initialState?.let {
            // probably `internal` modifier is very helpful here
            check(it.loadStates.sanitize() == it.loadStates) { "Intervention detected: persisted data is inconsistent with internal constraints" }
        }
    }

    // SAFETY: this channel is consumed by Pager
    val requestChannel = Channel<PagingRequest<Key>>(capacity = Channel.CONFLATED)
    // channelFlow is used to simplify channel provision and subsequent collection
    //
    // TODO it is possible to provision channel synchronously, which means safe state restoration code via DRY
    val flow = channelFlow {
        Pager(
            config = config,
            initialKey = initialKey,
            pagingSource = pagingSource,
            // if this particular line is extracted from constructor and delayed, code below will be safe as we can reuse Pager code
            // to create Snapshot instance
            // But delaying channel provision creates problems on its own
            snapshotChannel = channel,
            requestChannel = requestChannel,
            pages = initialState?.pages ?: emptyList<Page<Key, Value>>(),
            loadStates = initialState?.loadStates ?: defaultLoadStates()
        ).startPaging()
    }
    val sharedFlow = MutableSharedFlow<Snapshot<Key, Value>>(replay = 1)
    if (initialState != null) {
        debug {
            Log.d(TAG, "Got initial state, emitting synchronously")
        }
        // Synchronously create Snapshot and pass it to replayCache for PagingItemsImpl to pick it up also synchronously
        sharedFlow.tryEmit(
            // SAFETY: UNSAFE
            // See Pager comments on Snapshot creation (notifyObservers method)
            Snapshot(
                pages = initialState.pages,
                updateKind = UpdateKind.Refresh,
                pagingConfig = config,
                loadStates = initialState.loadStates,
                requestChannel = requestChannel
            )
        ).also {
            if (it == false) {
                Log.wtf(
                    TAG,
                    "Could not populate replay cache with initial state, this must never happen!"
                )
            }
        }

    }

    launch {
        // emulate SharingStarted.LAZILY
        sharedFlow.subscriptionCount.dropWhile { it == 0 }.first()
        // SAFETY: requestChannel reuse is prohibited by not exposing initial pager flow
        // and so this line is guaranteed to be the only collection of that flow.
        flow.collect(sharedFlow)
        Log.wtf(TAG, "Pager flow collection is completed, this must never happen!")
    }

    return sharedFlow.asSharedFlow().debug {
        check(initialState == null || replayCache.size == 1) { "Initial state is dropped, bailing out" }
    }
}

