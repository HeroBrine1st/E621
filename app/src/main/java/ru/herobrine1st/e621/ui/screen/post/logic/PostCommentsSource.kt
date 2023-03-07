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

package ru.herobrine1st.e621.ui.screen.post.logic

import android.util.Log
import androidx.compose.material.SnackbarDuration
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.fasterxml.jackson.core.JacksonException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.R
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.await
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.api.model.parseComments
import ru.herobrine1st.e621.ui.snackbar.SnackbarAdapter
import ru.herobrine1st.e621.util.JacksonExceptionHandler
import java.io.IOException
import kotlin.math.ceil
import kotlin.properties.Delegates

class PostCommentsSource(
    private val api: API,
    private val snackbar: SnackbarAdapter,
    private val jacksonExceptionHandler: JacksonExceptionHandler,
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
                val commentsRaw = withContext(Dispatchers.Default) {
                    parseComments(withContext(Dispatchers.IO) {
                        api.getCommentsForPostHTML(postId).await()
                    })
                }
                if (commentsRaw.isEmpty())
                    return LoadResult.Page(
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
        } catch (e: JacksonException) {
            jacksonExceptionHandler.handleDeserializationError(e)
            LoadResult.Error(e)
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