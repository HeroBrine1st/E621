package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.ApiException
import ru.herobrine1st.e621.api.IAPI
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.data.authorization.AuthorizationRepository
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.FavouritesCache
import ru.herobrine1st.e621.util.SearchOptions
import java.io.IOException
import java.util.function.Predicate
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val api: IAPI,
    private val snackbar: SnackbarAdapter,
    private val blacklistRepository: BlacklistRepository,
    private val favouritesCache: FavouritesCache,
    authorizationRepository: AuthorizationRepository
) : ViewModel() {
    private var searchOptions: SearchOptions? = null

    private val pager = Pager(
        PagingConfig(
            pageSize = BuildConfig.PAGER_PAGE_SIZE,
            initialLoadSize = BuildConfig.PAGER_PAGE_SIZE
        )
    ) {
        PostsSource(api, snackbar, searchOptions)
    }

    val isAuthorizedFlow = authorizationRepository.getAccountFlow().map { it != null }
    val usernameFlow = authorizationRepository.getAccountFlow().map { it?.login }

    private var blacklistPredicate by mutableStateOf<Predicate<Post>?>(null)
    val isBlacklistLoading get() = blacklistPredicate == null

    init {
        viewModelScope.launch {
            blacklistRepository.getEntriesFlow().map { list ->
                list
                    .filter { it.enabled }
                    .map { createTagProcessor(it.query) }
                    .fold(Predicate<Post> { false }) { a, b ->
                        a.or(b)
                    }
            }.collect {
                blacklistPredicate = it
            }
        }
    }

    // TODO make composable
    fun isHiddenByBlacklist(post: Post): Boolean {
        if (blacklistPredicate == null) return false
        return !favouritesCache.flow.value.getOrDefault(post.id, post.isFavorited)
                && blacklistPredicate!!.test(post)
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
                    if(wasFavourite) api.removeFromFavourites(post.id)
                    else api.addToFavourites(post.id)
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

    fun onSearchOptionsChange(
        searchOptions: SearchOptions,
        refresh: () -> Unit // looking for a way to do it right here
    ) {
        if (searchOptions == this.searchOptions) return
        this.searchOptions = searchOptions
        refresh()
    }

    companion object {
        const val TAG = "PostsViewModel"
    }
}