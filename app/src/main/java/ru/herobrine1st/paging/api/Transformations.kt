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
import kotlinx.coroutines.flow.shareIn
import ru.herobrine1st.paging.internal.Page

fun <Key : Any, Value : Any> Flow<Snapshot<Key, Value>>.cachedIn(scope: CoroutineScope) =
    shareIn(scope, SharingStarted.Lazily, replay = 1)

fun <Key : Any, Value : Any> SharedFlow<Snapshot<Key, Value>>.getStateForPreservation(): Pair<List<Page<Key, Value>>, LoadStates>? {
    val snapshot = this.replayCache.firstOrNull() ?: return null
    return snapshot.pages to snapshot.loadStates
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
        uiChannel = uiChannel
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