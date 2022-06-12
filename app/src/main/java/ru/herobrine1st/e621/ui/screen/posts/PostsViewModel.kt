package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.createTagProcessor
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.data.blacklist.BlacklistRepository
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.SearchOptions
import java.util.function.Predicate
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val api: Api,
    private val snackbar: SnackbarAdapter,
    private val blacklistRepository: BlacklistRepository
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

    fun isHiddenByBlacklist(post: Post): Boolean {
        // TODO handle cache of favourites
        if(blacklistPredicate == null) return false
        return !post.isFavorited && blacklistPredicate!!.test(post)
//                blacklistCache.entries.any {
//            it.dbEnabled && it.predicate.test(post)
//        }
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
}