package ru.herobrine1st.e621.ui.screen.post.logic

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.getCommentsForPost
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.await
import java.io.IOException
import kotlin.math.ceil
import kotlin.properties.Delegates

class PostCommentsSource(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val postId: Int
) : PagingSource<Int, Pair<CommentBB, PostReduced?>>() {
    // userId to post
    private lateinit var avatars: Map<Int, PostReduced?>
    private var firstPage by Delegates.notNull<Int>()

    override fun getRefreshKey(state: PagingState<Int, Pair<CommentBB, PostReduced?>>): Int? {
        return state.anchorPosition?.div(state.config.pageSize)?.plus(1)
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Pair<CommentBB, PostReduced?>> {

        return try {
            if (!::avatars.isInitialized) {
                val commentsRaw = withContext(Dispatchers.IO) {
                    api.getCommentsForPost(postId)
                }
                if (commentsRaw.isEmpty())
                    LoadResult.Page(
                        data = emptyList(),
                        nextKey = null,
                        prevKey = null
                    )
                avatars = withContext(Dispatchers.Default) {
                    commentsRaw.associateBy { it.authorId }.mapValues { it.value.avatarPost }
                }


                // Comments are reversed, doing my best to reverse it back
                firstPage =
                    ceil(commentsRaw.size.toDouble() / params.loadSize).toInt() // Kotlin/JVM wtf
            }
            val page = params.key ?: firstPage
            val limit = params.loadSize

            val res = withContext(Dispatchers.IO) {
                api.getCommentsForPostBBCode(postId, page, limit).await()
            }
            LoadResult.Page(
                data = res.map { it to avatars[it.creatorId] }.asReversed(),
                prevKey = if (page == firstPage) null else page + 1,
                nextKey = if (page == 1) null else page - 1,
            )
        } catch (e: IOException) {
            Log.e("PostCommentsSource", "Unable to load comments", e)
            snackbar.enqueueMessage(
                R.string.network_error,
                SnackbarDuration.Indefinite
            )
            LoadResult.Error(e)
        } catch (e: Throwable) {
            Log.e("Posts", "Unable to load comments", e)
            LoadResult.Error(e)
        }
    }

}