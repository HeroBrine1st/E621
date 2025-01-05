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

package ru.herobrine1st.paging.api

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.runBlocking
import ru.herobrine1st.paging.internal.Page
import ru.herobrine1st.paging.internal.SavedPagerState

/**
 * This function configures flow sharing in a way [PagingItems] can restore its state on subsequent instantiations via replayCache.
 *
 * @see waitStateRestorationAndCacheIn
 */
fun <Key : Any, Value : Any> Flow<Snapshot<Key, Value>>.cachedIn(scope: CoroutineScope) =
    shareIn(scope, SharingStarted.Lazily, replay = 1)

/**
 * This function returns pager state that can be saved to survive process recreation and then provided to [ru.herobrine1st.paging.createPager], enabling state preservation.
 *
 * The state preservation feature of Paging allows to save paging state across process recreations.
 * It is not a cache like [cachedIn]. Contrariwise, state preservation relies on state caching via synchronous cache population.
 *
 * This extension function is intended to be called on result of exactly [ru.herobrine1st.paging.createPager], otherwise **behavior** of state preservation feature **is undefined**.
 */
fun <Key : Any, Value : Any> SharedFlow<Snapshot<Key, Value>>.getStateForPreservation(): SavedPagerState<Key, Value>? {
    val snapshot = this.replayCache.firstOrNull() ?: return null
    if (snapshot.pages.isEmpty()) return null // empty pages - nothing to persist
    if (snapshot.loadStates.refresh !is LoadState.Complete) return null // if refreshing, current state will be invalid soon - nothing to persist
    return SavedPagerState(snapshot.pages, snapshot.loadStates.sanitize())
}

internal fun LoadStates.sanitize() = LoadStates(
    refresh = refresh.sanitize(),
    append = append.sanitize(),
    prepend = prepend.sanitize()
)

internal fun LoadState.sanitize() = when (this) {
    // Normal cases: no network activity
    LoadState.NotLoading -> this
    LoadState.Complete -> this

    // Edge cases
    is LoadState.Error -> LoadState.NotLoading
    LoadState.Loading -> LoadState.NotLoading
    LoadState.Idle -> error("Can't sanitize uninitialized state")
}

/**
 * This function configures flow sharing in a way [PagingItems] can restore its state on subsequent instantiations via replayCache,
 * waiting on transformations after state restoration is complete, mitigating first-frame issues at the cost of negligibly longer restoration time.
 *
 * This variant of [cachedIn] is intended to be used with [ru.herobrine1st.paging.createPager] variant that restores state after process recreation.
 * That variant caches flow itself, but it is not the case when
 * there are transformations between it and [PagingItems] and so flow is not cached anymore. This can be mitigated
 * by making them synchronised at the time of Pager creation, but it will not work if transformations
 * are dependent on other flows, e.g. from DataStore or Jetpack Room.
 *
 * This function fixes first-frame issues by waiting on first emission if [initialState] is not null,
 * assuming the same [initialState] is provided to [ru.herobrine1st.paging.createPager]. If violated, **behavior is undefined**.
 *
 * It is blocking, meaning all transformations between [ru.herobrine1st.paging.createPager] and this function
 * should be near-instant to avoid blocking process recreation. It is also possible to make dependencies
 * of those transformations cached themselves, which will further reduce state restoration time. In that case,
 * this function should not be used and currently no solution is provided to accommodate that.
 * You can see [ru.herobrine1st.paging.createPager] code on synchronous state restoration to make it yourself.
 *
 * This function has no performance impact on process starts without [initialState] available, and so
 * it is a good compromise: user waits negligibly longer, but is rewarded with restored state.
 *
 * @param scope coroutine scope to start sharing in.
 * @param initialState the same state provided to [ru.herobrine1st.paging.createPager]. If violated, **behavior is undefined**.
 */
fun <Key : Any, Value : Any> Flow<Snapshot<Key, Value>>.waitStateRestorationAndCacheIn(
    scope: CoroutineScope,
    initialState: SavedPagerState<*, *>?
): SharedFlow<Snapshot<Key, Value>> {
    // P.s. another solution is to set SharingStarter.Eagerly, but it implies race condition
    val flow = cachedIn(scope)
    if (initialState != null) {
        runBlocking { flow.first() }
    }
    return flow
}

inline fun <Key : Any, Value : Any, R : Any> Snapshot<Key, Value>.transform(
    block: (List<Value>) -> List<R>,
): Snapshot<Key, R> =
    // .copy(...) method can't change type parameter
    Snapshot(
        pages = pages.map {
            Page(
                key = it.key,
                prevKey = it.prevKey,
                nextKey = it.nextKey,
                data = block(it.data)
            )
        },
        updateKind = updateKind,
        pagingConfig = pagingConfig,
        loadStates = loadStates,
        requestChannel = requestChannel
    )

inline fun <Key : Any, Value : Any> Snapshot<Key, Value>.filter(block: (Value) -> Boolean) =
    transform {
        it.filter(block)
    }

inline fun <Key : Any, Value : Any, R : Any> Snapshot<Key, Value>.map(block: (Value) -> R) =
    transform {
        it.map(block)
    }

inline fun <Key : Any, Value : Any> Snapshot<Key, Value>.applyPageBoundary(
    crossinline block: (lastElementInPrevious: Value, firstElementInNext: Value) -> Pair<Value?, Value?>,
): Snapshot<Key, Value> {
    val resultingList = mutableListOf<MutableList<Value>>()
    val iterator = this.pages.iterator()

    while (iterator.hasNext()) {
        val next = iterator.next()
        resultingList.add(next.data.toMutableList())
        if (next.data.isNotEmpty()) break
    }
    iterator.forEach { page ->
        val previousPage = resultingList.last { it.isNotEmpty() }
        val currentPage = page.data.toMutableList()
        resultingList.add(currentPage)
        if (page.data.isNotEmpty()) {
            val lastElementInPrevious = previousPage.last()
            val firstElementInNext = currentPage.first()
            val (replacePrevious, replaceNext) = block(
                lastElementInPrevious,
                firstElementInNext
            )
            if (replacePrevious == null) previousPage.removeLast()
            else previousPage[previousPage.lastIndex] = replacePrevious
            if (replaceNext == null) currentPage.removeFirst()
            else currentPage[0] = replaceNext
        }
    }

    assert(resultingList.size == this.pages.size)

    return this.copy(
        pages = this.pages.zip(resultingList) { page, list ->
            page.copy(data = list)
        }
    )
}