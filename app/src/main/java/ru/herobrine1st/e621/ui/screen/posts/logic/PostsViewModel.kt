package ru.herobrine1st.e621.ui.screen.posts.logic

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.awaitResponse
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.SearchOptions
import java.io.IOException
import java.util.function.Predicate

class PostsViewModel @AssistedInject constructor(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val favouritesCache: FavouritesCache,
    @Assisted private val searchOptions: SearchOptions,
    blacklistRepository: BlacklistRepository,
) : ViewModel() {
    private val pager = Pager(
        PagingConfig(
            pageSize = BuildConfig.PAGER_PAGE_SIZE,
            initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
        )
    ) {
        PostsSource(api, snackbar, searchOptions)
    }

    private val blacklistPredicateFlow: StateFlow<Predicate<Post>?> =
        blacklistRepository.getEntriesFlow().map { list ->
            list.filter { it.enabled }
                .map { createTagProcessor(it.query) }
                .fold(Predicate<Post> { false }) { a, b ->
                    a.or(b)
                }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(),
            null
        )
    val isBlacklistLoading
        @Composable get() = blacklistPredicateFlow.collectAsState().value == null

    @Composable
    fun isHiddenByBlacklist(post: Post): Boolean {
        val predicate = blacklistPredicateFlow.collectAsState().value ?: return false
        val favourites by favouritesCache.flow.collectAsState()
        return !favourites.getOrDefault(post.id, post.isFavorited) // If not favourite
                && predicate.test(post) // And matches blacklist
    }

    @Composable
    fun isFavourite(post: Post) =
        favouritesCache.flow.collectAsState().value.getOrDefault(post.id, post.isFavorited)

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
                Log.e(TAG, "An API exception occurred", e)
            }
        }
    }

    val postsFlow: Flow<PagingData<Post>> = pager.flow.cachedIn(viewModelScope)

    val lazyListState = LazyListState(0, 0)

    private var countBlacklistedPosts = 0
    private var warnedUser = false

    /**
     * Very primitive detection of intersection between query and blacklist. It simply warns user about it.
     */
    fun notifyPostState(blacklisted: Boolean) {
        if (!blacklisted) countBlacklistedPosts = 0 else countBlacklistedPosts++
        if (countBlacklistedPosts > 300 && !warnedUser) {
            warnedUser = true
            Log.i("PostsViewModel", "Detected intersection between blacklist and query")
            viewModelScope.launch {
                snackbar.enqueueMessage(
                    R.string.maybe_search_query_intersects_with_blacklist,
                    SnackbarDuration.Indefinite
                )
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