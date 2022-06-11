package ru.herobrine1st.e621.ui.screen.posts

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.SnackbarDuration
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import ru.herobrine1st.e621.BuildConfig
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.Api
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.SearchOptions
import javax.inject.Inject

@HiltViewModel
class PostsViewModel @Inject constructor(
    private val api: Api,
    private val snackbar: SnackbarAdapter
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