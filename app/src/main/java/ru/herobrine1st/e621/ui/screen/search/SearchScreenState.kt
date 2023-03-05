/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.ui.screen.search

import android.os.Bundle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.util.getParcelableCompat

class SearchScreenState(
    initialPostsSearchOptions: PostsSearchOptions,
    initialCurrentlyModifiedTagIndex: Int = -1
) {
    val tags = initialPostsSearchOptions.tags.toMutableStateList()
    var order by mutableStateOf(initialPostsSearchOptions.order)
    var orderAscending by mutableStateOf(initialPostsSearchOptions.orderAscending)
    var rating = initialPostsSearchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialPostsSearchOptions.favouritesOf ?: "")

    // -2 = add new tag
    // -1 = idle
    // anything else = edit tag
    var currentlyModifiedTagIndex by mutableStateOf(initialCurrentlyModifiedTagIndex)

    fun makeSearchOptions(): PostsSearchOptions =
        PostsSearchOptions(tags.toList(), order, orderAscending, rating.toList(), favouritesOf
            .ifBlank { null })

    companion object {
        val Saver: Saver<SearchScreenState, Bundle> = Saver(
            save = { state ->
                with(state) {
                    val bundle = Bundle()
                    bundle.putParcelable(
                        PostsSearchOptions::class.simpleName,
                        makeSearchOptions()
                    )
                    bundle.putInt(
                        SearchScreenState::currentlyModifiedTagIndex.name,
                        currentlyModifiedTagIndex
                    )
                    return@Saver bundle
                }
            },
            restore = { bundle ->
                return@Saver SearchScreenState(
                    bundle.getParcelableCompat(PostsSearchOptions::class.simpleName)!!,
                    bundle.getInt(SearchScreenState::currentlyModifiedTagIndex.name)
                )
            }
        )
    }
}