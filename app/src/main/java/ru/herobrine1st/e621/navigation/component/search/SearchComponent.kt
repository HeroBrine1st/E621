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
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.api.AutocompleteSuggestionsAPI
import ru.herobrine1st.e621.api.Tokens
import ru.herobrine1st.e621.api.model.PostId
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
    private val dataStoreModule: DataStoreModule
) : ComponentContext by componentContext {

    private val lifecycleScope = LifecycleScope()

    val tags = initialSearchOptions.run {
        allOf.map { it.value } + anyOf.map { it.asAlternative } + noneOf.map { it.asExcluded }
    }.toMutableStateList()

    var order by mutableStateOf(initialSearchOptions.order)
    var orderAscending by mutableStateOf(initialSearchOptions.orderAscending)
    val rating = initialSearchOptions.rating.toMutableStateList()
    var favouritesOf by mutableStateOf(initialSearchOptions.favouritesOf ?: "")
    var fileType by mutableStateOf(initialSearchOptions.fileType)
    var fileTypeInvert by mutableStateOf(initialSearchOptions.fileTypeInvert)
    var parentPostId by mutableIntStateOf(initialSearchOptions.parent.value)
    var poolId by mutableIntStateOf(initialSearchOptions.poolId)

    @CachedDataStore
    val shouldBlockRatingChange
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.safeModeEnabled

    @CachedDataStore
    val shouldShowAccountFillInFavouritesOfField
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.auth != null && favouritesOf.isEmpty()

    fun onFavouritesOfTrailingButtonClick() {
        lifecycleScope.launch {
            if (favouritesOf.isNotEmpty()) {
                favouritesOf = ""
            } else {
                dataStoreModule.data.first().auth?.let {
                    favouritesOf = it.username
                }
            }

        }
    }

    fun tagSuggestionFlow(getCurrentText: () -> String): Flow<Autocomplete> {
        val currentTextFlow = snapshotFlow {
            getCurrentText()
                .lowercase()
                .trim()
        }
        return currentTextFlow
            .drop(1) // Ignore first as it is a starting tag (which is either an empty string or a valid tag)
            .conflate() // mapLatest would have no meaning: user should wait or no suggestions at all
            // delay handled by round-trip time and server (via retry plugin)
            // delaying here is wrong: response can be cached and so delay is pointless yet dispatched
            .combine(dataStoreModule.dataStore.data.map { it.autocompleteEnabled }) { query, isAutocompleteEnabled ->
                if (query.length < 3 || !isAutocompleteEnabled) {
                    return@combine Autocomplete.Ready(emptyList(), query)
                }

                api.getAutocompleteSuggestions(query).map {
                    it.map { suggestion ->
                        TagSuggestion(
                            name = suggestion.name,
                            postCount = suggestion.postCount,
                            antecedentName = suggestion.antecedentName
                        )
                    }
                }.map {
                    Autocomplete.Ready(it, query)
                }.recover {
                    exceptionReporter.handleRequestException(it, dontShowSnackbar = true)
                    Autocomplete.Error
                }.getOrThrow()
            }
            // Make it 🚀 turbo reactive 🚀
            .combine(currentTextFlow) { res, query ->
                when (res) {
                    is Autocomplete.Ready -> when (res.query) {
                        query -> res
                        else -> Autocomplete.Ready(
                            res.result.filter {
                                query in it.name.value || it.antecedentName?.value?.contains(
                                    query
                                ) == true
                            },
                            res.query
                        )
                    }

                    else -> res
                }
            }
            .flowOn(Dispatchers.Default)
    }


    constructor(
        componentContext: ComponentContext,
        navigator: StackNavigator<Config>,
        initialSearchOptions: PostsSearchOptions,
        api: AutocompleteSuggestionsAPI,
        exceptionReporter: ExceptionReporter,
        dataStoreModule: DataStoreModule
    ) : this(
        componentContext,
        componentContext.stateKeeper.consume(
            SEARCH_OPTIONS_STATE_KEY,
            strategy = PostsSearchOptions.serializer()
        )
            ?: initialSearchOptions,
        navigator,
        api,
        exceptionReporter,
        dataStoreModule
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
            fileTypeInvert = fileTypeInvert,
            parent = PostId(parentPostId),
            poolId = poolId
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

    sealed interface Autocomplete {
        data class Ready(val result: List<TagSuggestion>, val query: String) :
            Autocomplete

        data object Error : Autocomplete
    }
}