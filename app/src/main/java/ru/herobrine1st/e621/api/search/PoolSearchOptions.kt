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

package ru.herobrine1st.e621.api.search

import android.util.Log
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.E621_MAX_ITEMS_IN_RANGE_ENUMERATION
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId

@Serializable
data class PoolSearchOptions(
    val poolId: Int,
    private var postIds: List<PostId>? = null,
) : SearchOptions {
    override val maxLimit: Int get() = E621_MAX_ITEMS_IN_RANGE_ENUMERATION

    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        require(limit <= maxLimit)
        val postIds = (postIds ?: api.getPool(poolId).getOrThrow().posts)
            .also { postIds = it }
            .drop(limit * (page - 1))
            .take(limit)

        if (postIds.isEmpty()) return emptyList()

        // order:id for "normal" pools, i.e. first post in pool is first pool in result - no reverse required
        val posts =
            api.getPosts(tags = "id:${postIds.joinToString(",") { it.value.toString() }} order:id")
                .getOrThrow()
                .posts

        assert(posts.size == postIds.size) { "Expected ${postIds.size} posts, got ${posts.size}" }

        // FAST PATH: short-circuit if pool is "normal", which should be true for a vast number of pools
        // test that posts in response are in the same order as in postIds
        val isSorted = postIds
            .zip(posts) { id, post -> id == post.id }
            .all { it }
        if (isSorted) return posts

        // Or else, sort here
        val idToPost = posts.associateBy { it.id }
        return postIds.mapNotNull { id ->
            idToPost[id].also {
                // Just in case
                if (it == null) Log.w(
                    "PoolSearchOptions",
                    "API did not return post with id=$id for query with postIds=$postIds"
                )
            }
        }
    }
}