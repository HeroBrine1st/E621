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

package ru.herobrine1st.e621.navigation.component.posts

import android.util.Log
import androidx.annotation.IntRange
import androidx.compose.runtime.*
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.serializer
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.common.VoteResult
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.api.search.PoolSearchOptions
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.api.search.SearchOptions
import ru.herobrine1st.e621.database.repository.blacklist.BlacklistRepository
import ru.herobrine1st.e621.database.repository.vote.VoteRepository
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.module.PreferencesStore
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.*
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState.Determined.UNFAVOURITE
import ru.herobrine1st.paging.api.*
import ru.herobrine1st.paging.contrib.decompose.connectToDecomposeComponentAsPagingItems
import ru.herobrine1st.paging.contrib.decompose.consumePagingState
import ru.herobrine1st.paging.contrib.decompose.registerPagingState
import ru.herobrine1st.paging.createPager
import ru.herobrine1st.paging.internal.SavedPagerState
import java.util.function.Predicate

private const val TAG = "PostListingComponent"

class PostListingComponent(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val favouritesCache: FavouritesCache,
    private val exceptionReporter: ExceptionReporter,
    private val searchOptions: SearchOptions,
    private val navigator: StackNavigator<Config>,
    componentContext: ComponentContext,
    private val dataStoreModule: DataStoreModule,
    blacklistRepository: BlacklistRepository,
    private val voteRepository: VoteRepository,
) : ComponentContext by componentContext {

    private val lifecycleScope = LifecycleScope()
    private val instance: Instance = run {
        // Consume unconditionally to save memory
        val pagerState =
            stateKeeper.consumePagingState(
                "postPager",
                Int.serializer(),
                TransientPost.serializer()
            )
        debug {
            Log.d(TAG, "PagerState presence: ${pagerState != null}")
        }
        instanceKeeper.getOrCreate {
            Instance(
                api,
                favouritesCache,
                exceptionReporter,
                searchOptions,
                dataStoreModule.dataStore,
                blacklistRepository,
                pagerState
            )
        }.also { instance ->
            stateKeeper.registerPagingState(
                "postPager",
                Int.serializer(),
                TransientPost.serializer(),
                instance::pagerSupplier
            )
        }
    }

    val pagingItems = instance.postsFlow.connectToDecomposeComponentAsPagingItems(
        lifecycleScope,
        lifecycle,
        startImmediately = true
    )

    var infoState by mutableStateOf<InfoState>(InfoState.None)
        private set

    init {
        if (searchOptions is PoolSearchOptions) lifecycle.doOnResume(isOneTime = true) {
            val pool = searchOptions.pool
            lifecycleScope.launch(Dispatchers.Default) {
                infoState = InfoState.PoolInfo(pool, description = parseBBCode(pool.description))
            }
        }
    }

    fun onOpenPost(item: UIPostListingItem.Post, openComments: Boolean) {
        navigator.pushIndexed {
            Config.Post(
                id = item.post.id,
                post = item.post.originalPost,
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

    fun handleFavouriteChange(item: UIPostListingItem.Post) {
        lifecycleScope.launch {
            handleFavouriteChange(favouritesCache, api, snackbar, item.post)
        }
    }

    suspend fun vote(
        item: UIPostListingItem.Post,
        @IntRange(from = -1, to = 1) vote: Int
    ): VoteResult? {
        val postId = item.post.id
        val response = handleVote(postId, vote, api, exceptionReporter)
        if (response != null) voteRepository.setVote(postId, response.ourScore)
        return response?.let { VoteResult(it.ourScore, it.total) }
    }

    suspend fun getVote(item: UIPostListingItem.Post): Int? = voteRepository.getVote(item.post.id)

    @CachedDataStore
    val isAuthorized
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.auth != null

    @CachedDataStore
    val layoutPreference
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.postsColumns

    private class Instance(
        api: API,
        favouritesCache: FavouritesCache,
        exceptionReporter: ExceptionReporter,
        searchOptions: SearchOptions,
        dataStore: PreferencesStore,
        blacklistRepository: BlacklistRepository,
        pagerState: SavedPagerState<Int, TransientPost>?
    ) : InstanceBase() {
        private val blacklistPredicateFlow =
            blacklistRepository.getEntriesFlow()
                .map { list ->
                    list.filter { it.enabled }
                        .map { createTagProcessor(it.query) }
                        .reduceOrNull { a, b -> a.or(b) } ?: Predicate { false }
                }
                .flowOn(Dispatchers.Default)

        private val pager = lifecycleScope.createPager(
            PagingConfig(
                pageSize = BuildConfig.PAGER_PAGE_SIZE,
                prefetchDistance = BuildConfig.PAGER_PREFETCH_DISTANCE,
                initialLoadSize = BuildConfig.PAGER_PAGE_SIZE,
                maxPagesInMemory = BuildConfig.PAGER_MAX_PAGES_IN_MEMORY
            ),
            initialKey = 1,
            PostsSource(api, exceptionReporter, searchOptions),
            pagerState
        )

        fun pagerSupplier(): SharedFlow<Snapshot<Int, TransientPost>> {
            debug {
                Log.d("$TAG-Instance", "Got pager supply request")
            }
            return pager
        }

        val postsFlow = combine(
            pager,
            dataStore.data.map { it.blacklistEnabled },
            blacklistPredicateFlow,
            favouritesCache.flow,
            dataStore.data.map { it.safeModeEnabled }
        ) { posts, isBlacklistEnabled, blacklistPredicate, favourites, safeModeEnabled ->
            // It is hard to understand
            // Briefly, it maps Post to either show-able Post or hidden (due to blacklist or safe mode) item
            // then it merges sequences of hidden posts into one object
            // and then "one object" expands to series of objects - one info object and many empty
            // each having unique key corresponding to item in initial list
            //
            // Also it is probably possible to avoid re-computation when posts.updateKind is UpdateKind.StateChange,
            // but care should be taken not to avoid re-computation when other flows in combine emit new values
            posts.transform {
                it.map { post ->
                    when {
                        safeModeEnabled && post.rating != Rating.SAFE ->
                            IntermediatePostListingItem.HiddenItems.ofUnsafe(post)

                        isBlacklistEnabled && favourites.isFavourite(post) == UNFAVOURITE // Show post if it is either favourite
                                && blacklistPredicate.test(post) ->                       //           or is not blacklisted
                            IntermediatePostListingItem.HiddenItems.ofBlacklisted(post)

                        else -> IntermediatePostListingItem.Post(post)
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
            }.transform {
                it.flatMap(IntermediatePostListingItem::toUi)
            }
        }
            .flowOn(Dispatchers.Default)
            .waitStateRestorationAndCacheIn(lifecycleScope, pagerState)
    }

    sealed interface InfoState {
        data object None : InfoState

        data object Loading : InfoState

        data class PoolInfo(val pool: Pool, val description: List<MessageData>) : InfoState
    }
}