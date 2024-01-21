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
import ru.herobrine1st.e621.util.getAtIndex2DOrNull
import ru.herobrine1st.e621.util.indexOfFirst2D
import ru.herobrine1st.paging.api.Snapshot
import ru.herobrine1st.paging.internal.UpdateKind

inline fun <K : Any> Flow<Snapshot<K, PostListingItem>>.correctFirstVisibleItem(
    crossinline getFirstVisibleItemIndex: () -> Int,
    crossinline setIndex: (Int) -> Unit,
) = runningReduce { previous, current ->
    if (current.updateKind is UpdateKind.StateChange) return@runningReduce current

    val firstVisibleItemIndex = getFirstVisibleItemIndex()
    val currentlyObservedValue =
        previous.pages.getAtIndex2DOrNull(firstVisibleItemIndex) {
            it.data
        } ?: return@runningReduce current

    val index1 = when (currentlyObservedValue) {
        is PostListingItem.HiddenItems -> current.pages.indexOfFirst2D({ it.data }) {
            when (it) {
                is PostListingItem.HiddenItems -> (currentlyObservedValue.postIds == it.postIds).also { areEqual ->
                    if (areEqual) return@runningReduce current
                }

                is PostListingItem.Post -> currentlyObservedValue.postIds.first() == it.post.id
            }
        }

        is PostListingItem.Post -> {
            current.pages.indexOfFirst2D({ it.data }) {
                when (it) {
                    is PostListingItem.Post -> (it.key == currentlyObservedValue.key).also { areEqual ->
                        if (areEqual) return@runningReduce current
                    }

                    is PostListingItem.HiddenItems -> currentlyObservedValue.post.id in it.postIds
                }
            }
        }
    }
    Log.d(
        "Posts UI",
        "Snapping from key ${currentlyObservedValue.key} at index $firstVisibleItemIndex to index $index1"
    )
    setIndex(index1)

    current
}.flowOn(Dispatchers.Default)