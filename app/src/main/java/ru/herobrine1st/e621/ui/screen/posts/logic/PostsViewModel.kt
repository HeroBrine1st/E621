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

package ru.herobrine1st.e621.ui.screen.posts.logic

import android.content.Context
import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.*
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.preference.getPreferencesFlow
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.JacksonExceptionHandler
import java.io.IOException
import java.util.function.Predicate

class PostsViewModel @AssistedInject constructor(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val favouritesCache: FavouritesCache,
    private val jacksonExceptionHandler: JacksonExceptionHandler,
    @ApplicationContext applicationContext: Context,
    @Assisted private val searchOptions: SearchOptions,
    blacklistRepository: BlacklistRepository,
) : ViewModel() {
    private val pager = Pager(
        PagingConfig(
            pageSize = BuildConfig.PAGER_PAGE_SIZE,
            initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
        )
    ) {
        PostsSource(api, snackbar, jacksonExceptionHandler, searchOptions)
    }

    private val blacklistPredicateFlow =
        blacklistRepository.getEntriesFlow()
            .map { list ->
                list.filter { it.enabled }
                    .map { createTagProcessor(it.query) }
                    .reduceOrNull { a, b -> a.or(b) } ?: Predicate { false }
            }
            .flowOn(Dispatchers.Default)

    val postsFlow = combine(
        pager.flow.cachedIn(viewModelScope), // cachedIn strictly required here (double collection exception otherwise)
        applicationContext.getPreferencesFlow { it.blacklistEnabled },
        blacklistPredicateFlow,
        favouritesCache.flow
    ) { posts, isBlacklistEnabled, blacklistPredicate, favourites ->

        if (!isBlacklistEnabled) posts
        else posts.filter {
            favourites.getOrDefault(it.id, it.isFavorited) // Show post if it is either favourite
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


    @Composable
    fun collectFavouritesCacheAsState() = favouritesCache.flow.collectAsState()

    fun handleFavouriteButtonClick(post: Post) {
        viewModelScope.launch {
            val wasFavourite = favouritesCache.flow.value.getOrDefault(post.id, post.isFavorited)
            favouritesCache.setFavourite(post.id, !wasFavourite) // Instant UI reaction
            try {
                withContext(Dispatchers.IO) {
                    if (wasFavourite) api.removeFromFavourites(post.id).awaitResponse()
                    else api.addToFavourites(post.id).awaitResponse()
                }
            } catch (e: IOException) {
                favouritesCache.setFavourite(post.id, wasFavourite)
                Log.e(
                    TAG,
                    "IO Error while while trying to (un)favorite post (id=${post.id}, wasFavourite=$wasFavourite)",
                    e
                )
                snackbar.enqueueMessage(R.string.network_error, SnackbarDuration.Long)
            } catch (e: ApiException) {
                favouritesCache.setFavourite(post.id, wasFavourite)
                snackbar.enqueueMessage(R.string.unknown_api_error, SnackbarDuration.Long)
                Log.e(TAG, "An API exception occurred", e)
            }
        }
    }

    // Assisted inject stuff

    @AssistedFactory
    interface Factory {
        fun create(searchOptions: SearchOptions): PostsViewModel
    }

    @EntryPoint
    @InstallIn(ActivityComponent::class)
    interface FactoryProvider {
        @Suppress("INAPPLICABLE_JVM_NAME")
        @JvmName("providePostsViewModelFactory")
        fun provideFactory(): Factory
    }

    companion object {
        const val TAG = "PostsViewModel"

        @Suppress("UNCHECKED_CAST")
        fun provideFactory(
            assistedFactory: Factory,
            searchOptions: SearchOptions,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return assistedFactory.create(searchOptions) as T
            }
        }
    }
}