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

package ru.herobrine1st.e621.navigation.component.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.toMutableStateList
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ru.herobrine1st.autocomplete.AutocompleteSearchResult
import ru.herobrine1st.e621.api.AutocompleteSuggestionsAPI
import ru.herobrine1st.e621.api.Tokens
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.util.ExceptionReporter

const val SEARCH_OPTIONS_STATE_KEY = "SEARCH_COMPONENT_OPTIONS_STATE_KEY"


class SearchComponent private constructor(
    componentContext: ComponentContext,
    initialSearchOptions: PostsSearchOptions,
    private val navigator: StackNavigator<Config>,
    private val api: AutocompleteSuggestionsAPI,
    private val exceptionReporter: ExceptionReporter,
    private val dataStoreModule: DataStoreModule,
) : ComponentContext by componentContext {

    private val lifecycleScope = LifecycleScope()

    var presentedQuery: QueryPresentation by mutableStateOf(QueryPresentation.fromString(initialSearchOptions.query))

    private var _order by mutableStateOf(initialSearchOptions.order)
    var order
        get() = _order
        set(v) {
            if (v.ascendingApiName == null) orderAscending = false
            _order = v
        }
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    val rating = initialSearchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialSearchOptions.favouritesOf ?: "")
    val postTypes = initialSearchOptions.types.toMutableStateList()
    var parentPostId by mutableStateOf(initialSearchOptions.parent)
    var poolId by mutableIntStateOf(initialSearchOptions.poolId)

    @CachedDataStore
    val shouldBlockRatingChange
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.safeModeEnabled

    @CachedDataStore
    val accountName @Composable get() = dataStoreModule.cachedData.collectAsState().value.auth?.username

    fun tagSuggestionFlow(getCurrentText: () -> String): Flow<AutocompleteSearchResult<TagSuggestion>> {
        val currentTextFlow = snapshotFlow { getCurrentText() }
        return currentTextFlow
            .drop(1) // Ignore first as it is a starting tag (which is either an empty string or a valid tag)
            // delay handled by round-trip time and server (via retry plugin)
            // delaying here is wrong: response can be cached and so delay is pointless yet dispatched
            .combine(dataStoreModule.dataStore.data.map { it.autocompleteEnabled }) { query, isAutocompleteEnabled ->
                val cleanedQuery = query.lowercase().trim()
                    .removePrefix(Tokens.ALTERNATIVE)
                    .removePrefix(Tokens.EXCLUDED)
                if (cleanedQuery.length < 3 || !isAutocompleteEnabled) {
                    return@combine AutocompleteSearchResult.Ready(emptyList(), query)
                }

                api.getAutocompleteSuggestions(cleanedQuery).map {
                    AutocompleteSearchResult.Ready(
                        suggestions = it.map { suggestion ->
                            TagSuggestion(
                                name = suggestion.name,
                                postCount = suggestion.postCount,
                                antecedentName = suggestion.antecedentName,
                            )
                        },
                        query = query,
                    )
                }.recover {
                    exceptionReporter.handleRequestException(it, dontShowSnackbar = true)
                    AutocompleteSearchResult.Error
                }.getOrThrow()
            }
            // Make it 🚀 turbo reactive 🚀
            .combine(currentTextFlow) { res, query ->
                (res as? AutocompleteSearchResult.Ready)
                    ?.takeIf { it.query != query }
                    ?.let { res ->
                        res.copy(
                            suggestions = res.suggestions
                                .filter { suggestion ->
                                    query in suggestion.name.value
                                            || suggestion.antecedentName?.value?.contains(query) == true
                                },
                        )
                    }
                    ?: res

            }
            .flowOn(Dispatchers.Default)
    }


    constructor(
        componentContext: ComponentContext,
        navigator: StackNavigator<Config>,
        initialSearchOptions: PostsSearchOptions,
        api: AutocompleteSuggestionsAPI,
        exceptionReporter: ExceptionReporter,
        dataStoreModule: DataStoreModule,
    ) : this(
        componentContext,
        componentContext.stateKeeper.consume(
            SEARCH_OPTIONS_STATE_KEY,
            strategy = PostsSearchOptions.serializer(),
        )
            ?: initialSearchOptions,
        navigator,
        api,
        exceptionReporter,
        dataStoreModule,
    )

    init {
        stateKeeper.register(SEARCH_OPTIONS_STATE_KEY, strategy = PostsSearchOptions.serializer()) {
            makeSearchOptions()
        }

        // Update state on first frame to avoid triggering animations
        @OptIn(CachedDataStore::class) // SAFETY: Read-only operation
        if (dataStoreModule.cachedData.value.safeModeEnabled) {
            rating.apply {
                clear()
                add(Rating.SAFE)
            }
        }

        dataStoreModule.data
            .onEach {
                if (it.safeModeEnabled) {
                    rating.apply {
                        clear()
                        add(Rating.SAFE)
                    }
                }
            }
            .launchIn(lifecycleScope)
    }

    private fun makeSearchOptions(): PostsSearchOptions {
        return PostsSearchOptions(
            query = presentedQuery.query,
            order = order,
            orderAscending = orderAscending,
            rating = rating.toSet(),
            favouritesOf = favouritesOf.ifBlank { null },
            types = postTypes.toSet(),
            parent = parentPostId,
            poolId = poolId,
        )
    }

    fun proceed() {
        navigator.pushIndexed { Config.PostListing(makeSearchOptions(), index = it) }
    }

    data class TagSuggestion(
        val name: Tag,
        val postCount: Int,
        val antecedentName: Tag?,
    )

    sealed interface QueryPresentation {
        val query: String

        data class Raw(override val query: String) : QueryPresentation {
            // TODO reuse previous state to avoid recalculations
            //      can be done with custom copy and private constructor
            val canTransformToTagList get() = canParseQuery(query)
            fun toTagList(): TagList? = TagList.fromString(query)
        }

        data class TagList(val tags: List<String>) : QueryPresentation {
            override val query: String get() = tags.joinToString(" ")

            fun toRaw() = Raw(tags.joinToString(" "))

            companion object {
                fun fromString(query: String): TagList? {
                    if (!canParseQuery(query)) return null
                    return TagList(query.split(" ").filter { it.isNotBlank() })
                }
            }
        }

        companion object {
            fun fromString(query: String): QueryPresentation = TagList.fromString(query) ?: Raw(query)
        }
    }
}

private fun canParseQuery(query: String) = query.split(" ").none {
    // disallow groups
    it == "(" || it == "-(" || it == "~("
}