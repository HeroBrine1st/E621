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

package ru.herobrine1st.e621.navigation.component.posts


import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.search.SearchOptions
import ru.herobrine1st.e621.util.ExceptionReporter
import ru.herobrine1st.paging.api.LoadParams
import ru.herobrine1st.paging.api.LoadResult
import ru.herobrine1st.paging.api.PagingSource

class PostsSource(
    private val api: API,
    private val exceptionReporter: ExceptionReporter,
    private val searchOptions: SearchOptions?,
) : PagingSource<Int, Post> {
//    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
//        return state.anchorPosition?.div(state.config.pageSize)?.plus(1)
//    }

    override suspend fun getPage(params: LoadParams<Int>): LoadResult<Int, Post> {
        if (searchOptions == null) {
            return LoadResult.Page(
                emptyList(), null, null
            )
        }
        val limit = params.requestedSize.coerceAtMost(searchOptions.maxLimit)
        return try {
            val page = params.key
            val posts: List<Post> = searchOptions.getPosts(api, page = page, limit = limit)
            LoadResult.Page(
                data = posts,
                previousKey = if (page == 1) null else page - 1,
                nextKey = if (posts.isNotEmpty()) page + 1 else null
            )
        } catch (t: Throwable) {
            exceptionReporter.handleRequestException(t)
            LoadResult.Error(t)
        }
    }
}