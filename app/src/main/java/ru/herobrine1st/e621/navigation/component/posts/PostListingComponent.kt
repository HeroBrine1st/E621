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

package ru.herobrine1st.e621.navigation.component.posts

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.api.search.SearchOptions
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.paging.Pager
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.applyPageBoundary
import ru.herobrine1st.paging.api.cachedIn
import ru.herobrine1st.paging.api.transform
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState.Determined.UNFAVOURITE
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.accumulate
import ru.herobrine1st.e621.util.isFavourite
import java.util.function.Predicate

class PostListingComponent(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val favouritesCache: FavouritesCache,
    exceptionReporter: ExceptionReporter,
    private val searchOptions: SearchOptions,
    private val navigator: StackNavigator<Config>,
    componentContext: ComponentContext,
    applicationContext: Context,
    blacklistRepository: BlacklistRepository,
) : ComponentContext by componentContext {

    private val instance = instanceKeeper.getOrCreate {
        Instance(
            api,
            favouritesCache,
            exceptionReporter,
            searchOptions,
            applicationContext,
            blacklistRepository
        )
    }

    private val lifecycleScope = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob())

    init {
        lifecycle.doOnDestroy {
            lifecycleScope.cancel()
        }
    }

    fun onOpenPost(post: Post, openComments: Boolean) {
        navigator.pushIndexed {
            Config.Post(
                id = post.id,
                post = post,
                openComments = openComments,
                query = searchOptions,
                index = it
            )
        }
    }

    fun onOpenSearch() {
        navigator.pushIndexed { index ->
            Config.Search(
                initialSearch = PostsSearchOptions.from(searchOptions),
                index = index
            )
        }
    }

    @Composable
    fun collectFavouritesCacheAsState() = favouritesCache.flow.collectAsState()

    fun handleFavouriteChange(post: Post) {
        lifecycleScope.launch {
            handleFavouriteChange(favouritesCache, api, snackbar, post)
        }
    }

    val postsFlow get() = instance.postsFlow

    private class Instance(
        api: API,
        favouritesCache: FavouritesCache,
        exceptionReporter: ExceptionReporter,
        searchOptions: SearchOptions,
        applicationContext: Context,
        blacklistRepository: BlacklistRepository,
    ) : InstanceBase() {

        private val pager = Pager(
            PagingConfig(
                pageSize = BuildConfig.PAGER_PAGE_SIZE,
                prefetchDistance = BuildConfig.PAGER_PREFETCH_DISTANCE,
                initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
            ),
            initialKey = 1,
            PostsSource(api, exceptionReporter, searchOptions)
        )

        private val blacklistPredicateFlow =
            blacklistRepository.getEntriesFlow()
                .map { list ->
                    list.filter { it.enabled }
                        .map { createTagProcessor(it.query) }
                        .reduceOrNull { a, b -> a.or(b) } ?: Predicate { false }
                }
                .flowOn(Dispatchers.Default)

        val postsFlow = combine(
            pager.flow,
            applicationContext.getPreferencesFlow { it.blacklistEnabled },
            blacklistPredicateFlow,
            favouritesCache.flow,
            applicationContext.getPreferencesFlow { it.safeModeEnabled }
        ) { posts, isBlacklistEnabled, blacklistPredicate, favourites, safeModeEnabled ->
            // It is hard to understand
            // Briefly, it maps Post to either show-able Post or hidden (due to blacklist or safe mode) item
            // then it merges sequences of hidden posts into one object
            //
            // Also it is probably possible to avoid re-computation when posts.updateKind is UpdateKind.StateChange,
            // but care should be taken not to avoid re-computation when other flows in combine emit new values
            posts.transform {
                it.map { post ->
                    when {
                        safeModeEnabled && post.rating != Rating.SAFE ->
                            PostListingItem.HiddenItems.ofUnsafe(post)

                        isBlacklistEnabled && favourites.isFavourite(post) == UNFAVOURITE // Show post if it is either favourite
                                && blacklistPredicate.test(post) ->                       //           or is not blacklisted
                            PostListingItem.HiddenItems.ofBlacklisted(post)

                        else -> PostListingItem.Post(post)
                    }
                }.accumulate { previous, current ->
                    val (first, second) = mergePostListingItems(previous, current)
                    if (second != null) {
                        yield(first)
                        second
                    } else {
                        first
                    }
                }
            }.applyPageBoundary { lastElementInPrevious, firstElementInNext ->
                mergePostListingItems(lastElementInPrevious, firstElementInNext)
            }
        }
            .flowOn(Dispatchers.Default)
            .cachedIn(lifecycleScope)
    }
}

