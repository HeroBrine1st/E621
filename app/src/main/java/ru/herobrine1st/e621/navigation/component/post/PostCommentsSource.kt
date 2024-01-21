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

package ru.herobrine1st.e621.navigation.component.post

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.common.CommentData
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.api.model.parseCommentAvatarsAndGetCommentCount
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.paging.api.LoadParams
import ru.herobrine1st.paging.api.LoadResult
import ru.herobrine1st.paging.api.PagingSource
import kotlin.math.ceil
import kotlin.properties.Delegates

class PostCommentsSource(
    private val api: API,
    private val exceptionReporter: ExceptionReporter,
    private val postId: PostId,
) : PagingSource<Int, CommentData> {
    // commentId to post
    private lateinit var avatars: Map<Int, PostReduced?>
    private var firstPage by Delegates.notNull<Int>()

//    override fun getRefreshKey(state: PagingState<Int, CommentData>): Int? {
//        return state.anchorPosition?.div(state.config.pageSize)?.plus(1)
//    }

    override suspend fun getPage(params: LoadParams<Int>): LoadResult<Int, CommentData> {
        // Download user-to-avatar url mapping
        if (!::avatars.isInitialized) {
            api.getCommentsForPostHTML(postId)
                .map {
                    withContext(Dispatchers.Default) {
                        parseCommentAvatarsAndGetCommentCount(it)
                    }
                }
                .onSuccess {
                    val (avatars, commentCount) = it

                    if (commentCount == 0) return LoadResult.Page(
                        data = emptyList(),
                        nextKey = null,
                        previousKey = null
                    )

                    this.avatars = avatars
                    // comments are reversed
                    firstPage = ceil(commentCount.toDouble() / params.requestedSize).toInt()
                }
                .onFailure {
                    Log.e("Posts", "Unable to load comments", it)
                    exceptionReporter.handleRequestException(it)
                    return LoadResult.Error(it)
                }
        }

        val page = params.key.takeIf { it != Int.MIN_VALUE } ?: firstPage
        val limit = params.requestedSize

        return api.getCommentsForPostBBCode(postId, page, limit).map { it ->
            it.asReversed()
        }.map {
            withContext(Dispatchers.Default) {
                it.map {
                    CommentData.fromE621Comment(it, avatars[it.id])
                }
            }
        }.map {
            LoadResult.Page(
                data = it,
                previousKey = if (page == firstPage) null else page + 1,
                nextKey = if (page == 1) null else page - 1,
            )
        }.recover {
            Log.e("Posts", "Unable to load comments", it)
            exceptionReporter.handleRequestException(it)
            LoadResult.Error(it)
        }.getOrThrow()

    }

}