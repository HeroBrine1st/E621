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
import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import androidx.paging.filter
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
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.FavouritesSearchOptions
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.awaitSuccessfulResponse
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.navigation.config.Config
import ru.herobrine1st.e621.navigation.pushIndexed
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.theme.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.FavouritesCache.FavouriteState
import ru.herobrine1st.e621.util.InstanceBase
import java.io.IOException
import java.util.function.Predicate

private const val TAG = "PostListingComponent"

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
            snackbar,
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
                initialSearch = when (searchOptions) {
                    is PostsSearchOptions -> searchOptions
                    is FavouritesSearchOptions -> PostsSearchOptions(
                        favouritesOf = searchOptions.favouritesOf
                    )
                },
                index = index
            )
        }
    }

    @Composable
    fun collectFavouritesCacheAsState() = favouritesCache.flow.collectAsState()

    fun handleFavouriteButtonClick(post: Post) {
        lifecycleScope.launch {
            val wasFavourite: FavouriteState = favouritesCache.isFavourite(post)
            when (wasFavourite) {
                is FavouriteState.InFly -> {
                    Log.w(
                        TAG,
                        "Inconsistent state: \"favourite\" button was clicked while should be disabled"
                    )
                    return@launch
                }
                // Smart cast is not that smart
                is FavouriteState.Determined -> {}
            }
            // Instant UI reaction
            favouritesCache.setFavourite(post.id, FavouriteState.InFly(wasFavourite))
            try {
                if (wasFavourite.isFavourite) api.removeFromFavourites(post.id)
                    .awaitSuccessfulResponse()
                else api.addToFavourites(post.id).awaitSuccessfulResponse()
                favouritesCache.setFavourite(
                    post.id,
                    FavouriteState.Determined.fromBoolean(!wasFavourite.isFavourite)
                )
            } catch (e: ApiException) {
                // TODO this can also occur when post is already (un)favourite
                favouritesCache.setFavourite(post.id, wasFavourite)
                snackbar.enqueueMessage(R.string.unknown_api_error, SnackbarDuration.Long)
                Log.e(TAG, "An API exception occurred", e)
            } catch (e: IOException) {
                favouritesCache.setFavourite(post.id, wasFavourite)
                Log.e(
                    TAG,
                    "IO Error while while trying to (un)favorite post (id=${post.id}, wasFavourite=$wasFavourite)",
                    e
                )
                snackbar.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
            }
        }
    }

    val postsFlow get() = instance.postsFlow

    private class Instance(
        api: API,
        snackbar: SnackbarAdapter,
        favouritesCache: FavouritesCache,
        exceptionReporter: ExceptionReporter,
        searchOptions: SearchOptions,
        applicationContext: Context,
        blacklistRepository: BlacklistRepository,
    ) : InstanceBase() {

        private val pager = Pager(
            PagingConfig(
                pageSize = BuildConfig.PAGER_PAGE_SIZE,
                initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
            )
        ) {
            PostsSource(api, snackbar, exceptionReporter, searchOptions)
        }

        private val blacklistPredicateFlow =
            blacklistRepository.getEntriesFlow()
                .map { list ->
                    list.filter { it.enabled }
                        .map { createTagProcessor(it.query) }
                        .reduceOrNull { a, b -> ;a.or(b) } ?: Predicate { false }
                }
                .flowOn(Dispatchers.Default)

        val postsFlow = combine(
            pager.flow.cachedIn(lifecycleScope), // cachedIn strictly required here (double collection exception otherwise)
            applicationContext.getPreferencesFlow { it.blacklistEnabled },
            blacklistPredicateFlow,
            favouritesCache.flow
        ) { posts, isBlacklistEnabled, blacklistPredicate, favourites ->
            if (!isBlacklistEnabled) posts
            else posts.filter {
                favourites.getOrDefault(
                    it.id,
                    FavouriteState.Determined.fromBoolean(it.isFavorited)
                ) != FavouriteState.Determined.UNFAVOURITE // Show post if it is either favourite
                        || !blacklistPredicate.test(it)        //           or is not blacklisted
            }
        }.combine(applicationContext.getPreferencesFlow { it.safeModeEnabled }) { posts, safeModeEnabled ->
            if (safeModeEnabled) posts.filter { it.rating == Rating.SAFE }
            else posts
            /* CPU intensive ?
         * Does it need to be cached? I mean, in terms of memory and CPU resources
         * And, maybe, energy.
         */
        }.flowOn(Dispatchers.Default)
    }
}