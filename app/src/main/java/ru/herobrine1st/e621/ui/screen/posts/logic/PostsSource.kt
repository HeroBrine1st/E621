package ru.herobrine1st.e621.ui.screen.posts.logic

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import java.io.IOException

class PostsSource(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val searchOptions: SearchOptions?,
) : PagingSource<Int, Post>() {
    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        return state.anchorPosition?.div(state.config.pageSize)?.plus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        if (searchOptions == null) {
            return LoadResult.Page(
                emptyList(), null, null
            )
        }
        return try {
            val page = params.key ?: 1


            val posts: List<Post> = withContext(Dispatchers.IO) {
                @Suppress("BlockingMethodInNonBlockingContext") // False positive
                searchOptions.getPosts(api, page = page, limit = params.loadSize)
            }
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isNotEmpty()) page + 1 else null
            )
        } catch (e: IOException) {
            Log.e("Posts", "Unable to load posts", e)
            snackbar.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
            LoadResult.Error(e)
        } catch (e: Throwable) {
            Log.e("Posts", "Unable to load posts", e)
            LoadResult.Error(e)
        }
    }
}