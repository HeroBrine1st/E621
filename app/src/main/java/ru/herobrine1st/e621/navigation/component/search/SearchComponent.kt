/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

package ru.herobrine1st.e621.navigation.component.search

import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.statekeeper.consume
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed

const val STATE_KEY = "SEARCH_COMPONENT_STATE_KEY"

class SearchComponent private constructor(
    componentContext: ComponentContext,
    initialState: StateParcelable,
    private val navigator: StackNavigator<Config>,
) : ComponentContext by componentContext {
    val tags = initialState.searchOptions.tags.toMutableStateList()

    var order by mutableStateOf(initialState.searchOptions.order)
    var orderAscending by mutableStateOf(initialState.searchOptions.orderAscending)
    val rating = initialState.searchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialState.searchOptions.favouritesOf ?: "")
    var fileType by mutableStateOf(initialState.searchOptions.fileType)
    var fileTypeInvert by mutableStateOf(initialState.searchOptions.fileTypeInvert)

    // -2 = add new tag
    // -1 = idle
    // anything else = edit tag
    var currentlyModifiedTagIndex by mutableStateOf(initialState.currentlyModifiedTagIndex)

    constructor(
        componentContext: ComponentContext,
        navigator: StackNavigator<Config>,
        initialPostsSearchOptions: PostsSearchOptions,
        initialCurrentlyModifiedTagIndex: Int = -1
    ) : this(
        componentContext,
        componentContext.stateKeeper.consume(STATE_KEY)
            ?: StateParcelable(initialPostsSearchOptions, initialCurrentlyModifiedTagIndex),
        navigator
    )

    init {
        stateKeeper.register(STATE_KEY) {
            StateParcelable(makeSearchOptions(), currentlyModifiedTagIndex)
        }
    }

    private fun makeSearchOptions(): PostsSearchOptions =
        PostsSearchOptions(
            tags.toList(), order, orderAscending, rating.toList(), favouritesOf
                .ifBlank { null }, fileType, fileTypeInvert
        )

    fun proceed() {
        navigator.pushIndexed { Config.PostListing(makeSearchOptions(), index = it) }
    }

    @Parcelize
    private data class StateParcelable(
        val searchOptions: PostsSearchOptions,
        val currentlyModifiedTagIndex: Int
    ) : Parcelable
}