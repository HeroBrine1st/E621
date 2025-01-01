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

import androidx.annotation.IntRange
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.StackNavigator
import com.arkivanov.essenty.instancekeeper.getOrCreate
import com.arkivanov.essenty.lifecycle.doOnResume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.common.VoteResult
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Pool
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.parseBBCode
import ru.herobrine1st.e621.api.search.PoolSearchOptions
import ru.herobrine1st.e621.api.search.PostsSearchOptions
import ru.herobrine1st.e621.api.search.SearchOptions
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.data.vote.VoteRepository
import ru.herobrine1st.e621.module.CachedDataStore
import ru.herobrine1st.e621.module.DataStoreModule
import ru.herobrine1st.e621.module.PreferencesStore
import ru.herobrine1st.e621.navigation.LifecycleScope
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState.Determined.UNFAVOURITE
import ru.herobrine1st.e621.util.InstanceBase
import ru.herobrine1st.e621.util.accumulate
import ru.herobrine1st.e621.util.isFavourite
import ru.herobrine1st.paging.api.PagingConfig
import ru.herobrine1st.paging.api.applyPageBoundary
import ru.herobrine1st.paging.api.cachedIn
import ru.herobrine1st.paging.api.transform
import ru.herobrine1st.paging.createPager
import java.util.function.Predicate

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

    val postsFlow get() = instance.postsFlow

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


    private val instance = instanceKeeper.getOrCreate {
        Instance(
            api,
            favouritesCache,
            exceptionReporter,
            searchOptions,
            dataStoreModule.dataStore,
            blacklistRepository
        )
    }

    private val lifecycleScope = LifecycleScope()

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

    suspend fun vote(postId: PostId, @IntRange(from = -1, to = 1) vote: Int): VoteResult? {
        val response = handleVote(postId, vote, api, exceptionReporter)
        if (response != null) voteRepository.setVote(postId, response.ourScore)
        return response?.let { VoteResult(it.ourScore, it.total) }
    }

    suspend fun getVote(postId: PostId): Int? = voteRepository.getVote(postId)

    @CachedDataStore
    val isAuthorized
        @Composable
        get() = dataStoreModule.cachedData.collectAsState().value.auth != null

    private class Instance(
        api: API,
        favouritesCache: FavouritesCache,
        exceptionReporter: ExceptionReporter,
        searchOptions: SearchOptions,
        dataStore: PreferencesStore,
        blacklistRepository: BlacklistRepository,
    ) : InstanceBase() {
        private val blacklistPredicateFlow =
            blacklistRepository.getEntriesFlow()
                .map { list ->
                    list.filter { it.enabled }
                        .map { createTagProcessor(it.query) }
                        .reduceOrNull { a, b -> a.or(b) } ?: Predicate { false }
                }
                .flowOn(Dispatchers.Default)

        val postsFlow = combine(
            createPager(
                PagingConfig(
                    pageSize = BuildConfig.PAGER_PAGE_SIZE,
                    prefetchDistance = BuildConfig.PAGER_PREFETCH_DISTANCE,
                    initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
                ),
                initialKey = 1,
                PostsSource(api, exceptionReporter, searchOptions)
            ),
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
                            InternalPostListingItem.HiddenItems.ofUnsafe(post)

                        isBlacklistEnabled && favourites.isFavourite(post) == UNFAVOURITE // Show post if it is either favourite
                                && blacklistPredicate.test(post) ->                       //           or is not blacklisted
                            InternalPostListingItem.HiddenItems.ofBlacklisted(post)

                        else -> InternalPostListingItem.Post(post)
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
                it.flatMap<InternalPostListingItem, UIPostListingItem> { item ->
                    when (item) {
                        is InternalPostListingItem.HiddenItems -> {
                            buildList(item.postIds.size) {
                                add(
                                    UIPostListingItem.HiddenItemsBridge(
                                        id = item.postIds[0],
                                        hiddenDueToBlacklistNumber = item.hiddenDueToBlacklistNumber,
                                        hiddenDueToSafeModeNumber = item.hiddenDueToSafeModeNumber
                                    )
                                )
                                item.postIds.drop(1).map { id ->
                                    UIPostListingItem.Empty(id)
                                }.let { list ->
                                    addAll(list)
                                }
                            }
                        }

                        is InternalPostListingItem.Post -> {
                            listOf(UIPostListingItem.Post(item.post))
                        }
                    }
                }
            }
        }
            .flowOn(Dispatchers.Default)
            .cachedIn(lifecycleScope)
    }

    sealed interface InfoState {
        data object None : InfoState

        data object Loading : InfoState

        data class PoolInfo(val pool: Pool, val description: List<MessageData>) : InfoState
    }
}

