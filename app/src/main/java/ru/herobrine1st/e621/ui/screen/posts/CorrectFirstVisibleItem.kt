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

package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.runningReduce
import ru.herobrine1st.e621.navigation.component.posts.PostListingItem
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.getAtIndex2DOrNull
import ru.herobrine1st.e621.util.indexOfFirst2D
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.internal.UpdateKind

data class IndexContainer(
    /**
     * Generation number is used to synchronize [correctFirstVisibleItem] with effects
     * that will use [index] to restore scroll position
     */
    val generation: Int,
    /**
     * Index to snap to
     */
    val index: Int,
)

/**
 * This middleware reacts to structural changes in underlying data to update scrolling
 * index with the same location in the new data.
 *
 * Some of structural changes are beyond LazyList capabilities and there's no API
 * to override internal key-to-index maps or alter index restoration behavior.
 *
 * Detected changes follow:
 *
 * * Series of elements replaced with one element with composite key
 * * Element with compose key replaced with series of elements of keys that former element's key composed of
 *
 * Changes that LazyList can recover from are detected and ignored.
 *
 * @param getFirstVisibleItemIndex Lambda to get LazyListState.firstVisibleItemIndex
 * @param setIndex lambda to save data about new scroll index
 */
inline fun <K : Any> Flow<Snapshot<K, PostListingItem>>.correctFirstVisibleItem(
    crossinline getFirstVisibleItemIndex: () -> Int,
    crossinline setIndex: (IndexContainer) -> Unit,
) = runningReduce { previous, current ->
    if (current.updateKind is UpdateKind.StateChange) return@runningReduce current

    val firstVisibleItemIndex = getFirstVisibleItemIndex()
    val currentlyObservedValue =
        previous.pages.getAtIndex2DOrNull(firstVisibleItemIndex) {
            it.data
        } ?: return@runningReduce current


    val index = when (currentlyObservedValue) {
        is PostListingItem.HiddenItems -> current.pages.indexOfFirst2D({ it.data }) {
            when (it) {
                is PostListingItem.HiddenItems -> (currentlyObservedValue.key == it.key).also { areEqual ->
                    if (areEqual) return@runningReduce current // LazyList will recover itself
                }

                is PostListingItem.Post -> currentlyObservedValue.postIds.first() == it.post.id
            }
        }

        is PostListingItem.Post -> {
            current.pages.indexOfFirst2D({ it.data }) {
                when (it) {
                    is PostListingItem.Post -> (it.key == currentlyObservedValue.key).also { areEqual ->
                        if (areEqual) return@runningReduce current // LazyList will recover itself
                    }

                    is PostListingItem.HiddenItems -> currentlyObservedValue.post.id in it.postIds
                }
            }
        }
    }
    if (index == -1) return@runningReduce current
    debug {
        Log.d(
            "CorrectFirstVisibleItem",
            "Snapping from key ${currentlyObservedValue.key} at index $firstVisibleItemIndex to index $index (generation ${current.generation})"
        )
    }
    setIndex(IndexContainer(generation = current.generation, index = index))

    current
}.flowOn(Dispatchers.Default)