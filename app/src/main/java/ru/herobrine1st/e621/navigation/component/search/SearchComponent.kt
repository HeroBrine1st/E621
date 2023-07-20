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
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.Tokens
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.dataStore
import ru.herobrine1st.e621.preference.getPreferencesFlow

const val SEARCH_OPTIONS_STATE_KEY = "SEARCH_COMPONENT_OPTIONS_STATE_KEY"

class SearchComponent private constructor(
    componentContext: ComponentContext,
    initialSearchOptions: PostsSearchOptions,
    private val navigator: StackNavigator<Config>,
    private val api: API,
    applicationContext: Context
) : ComponentContext by componentContext {

    private val dataStore = applicationContext.dataStore

    val tags = initialSearchOptions.run {
        allOf.map { it.value } + anyOf.map { it.asAlternative } + noneOf.map { it.asExcluded }
    }.toMutableStateList()

    var order by mutableStateOf(initialSearchOptions.order)
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    val rating = initialSearchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialSearchOptions.favouritesOf ?: "")
    var fileType by mutableStateOf(initialSearchOptions.fileType)
    var fileTypeInvert by mutableStateOf(initialSearchOptions.fileTypeInvert)

    fun tagSuggestionFlow(getCurrentText: () -> String): Flow<List<TagSuggestion>> {
        val currentTextFlow = snapshotFlow {
            getCurrentText()
                .lowercase()
                .trim()
        }
        return currentTextFlow
            .drop(1) // Ignore first as it is a starting tag (which is either an empty string or a valid tag)
            .conflate() // mapLatest would have no meaning: user should wait or no suggestions at all
            // Delay is handled by interceptor
            .combine(dataStore.getPreferencesFlow { it.autocompleteEnabled }) { query, isAutocompleteEnabled ->
                if (query.length < 3 || !isAutocompleteEnabled) return@combine emptyList()
                api.getAutocompleteSuggestions(query).await().map {
                    TagSuggestion(
                        name = it.name,
                        postCount = it.postCount,
                        antecedentName = it.antecedentName
                    )
                }
            }
            // Make it 🚀 turbo reactive 🚀
            .combine(currentTextFlow) { suggestions, query ->
                suggestions.filter {
                    query in it.name.value || it.antecedentName?.value?.contains(
                        query
                    ) == true
                }
            }
            .flowOn(Dispatchers.Default)
    }

    constructor(
        componentContext: ComponentContext,
        navigator: StackNavigator<Config>,
        initialSearchOptions: PostsSearchOptions,
        api: API,
        applicationContext: Context
    ) : this(
        componentContext,
        componentContext.stateKeeper.consume(SEARCH_OPTIONS_STATE_KEY)
            ?: initialSearchOptions,
        navigator,
        api,
        applicationContext
    )

    init {
        stateKeeper.register(SEARCH_OPTIONS_STATE_KEY) {
            makeSearchOptions()
        }
    }

    private fun makeSearchOptions(): PostsSearchOptions {
        val noneOf = tags.filter { it.startsWith(Tokens.EXCLUDED) }
        val anyOf = tags.filter { it.startsWith(Tokens.ALTERNATIVE) }

        val allOf = tags - noneOf.toSet() - anyOf.toSet()
        // O(n) implementation, because lists are ordered
        // But there won't be much items, so that it commented out because it may be erroneous..
//        val allOf = tags.let {
//            var noneOfIndex = 0
//            var anyOfIndex = 0
//            val result = mutableSetOf<Tag>()
//            for(i in it.indices) {
//                if(it[i] == noneOf[noneOfIndex]) {
//                    noneOfIndex++
//                } else if(it[i] == anyOf[anyOfIndex]) {
//                    anyOfIndex++
//                } else {
//                    result += Tag(it[i])
//                }
//            }
//            return@let result
//        }

        return PostsSearchOptions(
            allOf = allOf.mapTo(mutableSetOf()) { Tag(it) },
            noneOf = noneOf.mapTo(mutableSetOf()) { Tag(it.substring(1)) },
            anyOf = anyOf.mapTo(mutableSetOf()) { Tag(it.substring(1)) },
            order = order,
            orderAscending = orderAscending,
            rating = rating.toSet(),
            favouritesOf = favouritesOf.ifBlank { null },
            fileType = fileType,
            fileTypeInvert = fileTypeInvert
        )
    }

    fun proceed() {
        navigator.pushIndexed { Config.PostListing(makeSearchOptions(), index = it) }
    }

    data class TagSuggestion(
        val name: Tag,
        val postCount: Int,
        val antecedentName: Tag?
    )
}