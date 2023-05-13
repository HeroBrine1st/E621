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

import android.content.Context
import android.os.Parcelable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.statekeeper.consume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.util.normalizeTagForAPI

const val STATE_KEY = "SEARCH_COMPONENT_STATE_KEY"

class SearchComponent private constructor(
    componentContext: ComponentContext,
    initialState: StateParcelable,
    private val navigator: StackNavigator<Config>,
    private val api: API,
    applicationContext: Context
) : ComponentContext by componentContext {

    private val _tags = initialState.searchOptions.tags.toMutableStateList()
    val tags: List<String> by ::_tags

    var order by mutableStateOf(initialState.searchOptions.order)
    var orderAscending by mutableStateOf(initialState.searchOptions.orderAscending)
    val rating = initialState.searchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialState.searchOptions.favouritesOf ?: "")
    var fileType by mutableStateOf(initialState.searchOptions.fileType)
    var fileTypeInvert by mutableStateOf(initialState.searchOptions.fileTypeInvert)

    var tagModificationState by mutableStateOf(initialState.tagModificationState)
        private set
    var tagModificationText by mutableStateOf("")

    val tagSuggestionFlow: Flow<List<TagSuggestion>> =
        // TODO handle ~, - and ' ' with object-oriented approach
        snapshotFlow { tagModificationText.trimStart('~', '-').normalizeTagForAPI() }
            .drop(1) // Ignore first as it is a starting tag (which is either an empty string or a valid tag)
            .conflate() // mapLatest would have no meaning: user should wait or no suggestions at all
            // Delay is handled by interceptor
            .combine(applicationContext.getPreferencesFlow { it.autocompleteEnabled }) { query, isAutocompleteEnabled ->
                if (query.length < 3 || query.isBlank() || !isAutocompleteEnabled) return@combine emptyList()
                api.getAutocompleteSuggestions(query).await().map {
                    TagSuggestion(
                        name = it.name,
                        postCount = it.postCount,
                        antecedentName = it.antecedentName
                    )
                }
            }
            // Make it 🚀 turbo reactive 🚀
            .combine(snapshotFlow {
                tagModificationText.trimStart('~', '-').normalizeTagForAPI()
            }) { suggestions, query ->
                suggestions.filter { query in it.name || it.antecedentName?.contains(query) == true }
            }
            .flowOn(Dispatchers.Default)

    constructor(
        componentContext: ComponentContext,
        navigator: StackNavigator<Config>,
        initialPostsSearchOptions: PostsSearchOptions,
        api: API,
        applicationContext: Context
    ) : this(
        componentContext,
        componentContext.stateKeeper.consume(STATE_KEY)
            ?: StateParcelable(initialPostsSearchOptions),
        navigator,
        api,
        applicationContext
    )

    init {
        stateKeeper.register(STATE_KEY) {
            StateParcelable(makeSearchOptions(), tagModificationState)
        }
    }

    private fun makeSearchOptions(): PostsSearchOptions =
        PostsSearchOptions(
            _tags.toList(), order, orderAscending, rating.toList(), favouritesOf
                .ifBlank { null }, fileType, fileTypeInvert
        )

    fun proceed() {
        navigator.pushIndexed { Config.PostListing(makeSearchOptions(), index = it) }
    }

    fun openAddTagDialog() {
        tagModificationText = ""
        tagModificationState = TagModificationState.AddingNew
    }

    fun openEditTagDialog(index: Int) {
        tagModificationText = _tags[index]
        tagModificationState = TagModificationState.Editing(index)
    }

    fun finishTagModification() {
        when (val state = tagModificationState) {
            is TagModificationState.Editing -> {
                _tags[state.index] = tagModificationText.normalizeTagForAPI()
            }

            else -> _tags.add(tagModificationText.normalizeTagForAPI())
        }
        tagModificationState = TagModificationState.None
        tagModificationText = ""
    }

    fun cancelTagModification() {
        tagModificationState = TagModificationState.None
        tagModificationText = ""
    }

    fun deleteCurrentlyModifiedTag() {
        _tags.removeAt((tagModificationState as TagModificationState.Editing).index)
        tagModificationState = TagModificationState.None
        tagModificationText = ""
    }

    @Parcelize
    private data class StateParcelable(
        val searchOptions: PostsSearchOptions,
        val tagModificationState: TagModificationState = TagModificationState.None
    ) : Parcelable

    @Parcelize
    sealed interface TagModificationState : Parcelable {
        @Parcelize
        object None : TagModificationState

        @Parcelize
        object AddingNew : TagModificationState

        @Parcelize
        class Editing(val index: Int) : TagModificationState
    }

    data class TagSuggestion(
        val name: String,
        val postCount: Int,
        val antecedentName: String?
    )
}