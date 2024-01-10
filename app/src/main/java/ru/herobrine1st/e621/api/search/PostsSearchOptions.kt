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
import ru.herobrine1st.e621.api.E621_MAX_POSTS_IN_QUERY
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.PoolId
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.util.debug

@Serializable
data class PostsSearchOptions(
    val allOf: Set<Tag> = emptySet(),
    val noneOf: Set<Tag> = emptySet(),
    val anyOf: Set<Tag> = emptySet(),
    val order: Order = Order.NEWEST_TO_OLDEST,
    val orderAscending: Boolean = false,
    val rating: Set<Rating> = emptySet(),
    val favouritesOf: String? = null, // "favorited_by" in api
    val fileType: FileType? = null,
    val fileTypeInvert: Boolean = false,
    val parent: PostId = PostId.INVALID,
    val poolId: PoolId = -1,
) : SearchOptions {
    // TODO randomSeed or something like that for Order.RANDOM

    private fun compileToQuery(): String {
        val cache = mutableListOf<String>()

        cache += allOf.map { it.value }
        cache += noneOf.map { it.asExcluded }
        cache += anyOf.map { it.asAlternative }
        cache += optimizeRatingSelection(rating)
        fileType?.extension?.let { cache += (if (fileTypeInvert) "-" else "") + "type:" + it }
        favouritesOf?.let { cache += "fav:$it" }
        (if (orderAscending) order.ascendingApiName else order.apiName)?.let { cache += "order:$it" }
        if (parent != PostId.INVALID) cache += "parent:${parent.value}"
        if (poolId > 0) cache += "pool:$poolId"

        return cache.joinToString(" ").debug {
            Log.d(PostsSearchOptions::class.simpleName, "Built query: $this")
        }
    }

    private fun optimizeRatingSelection(
        selection: Collection<Rating>,
    ): List<String> {
        val isOptimizationRequired = selection.size > Rating.entries.size / 2
        val minima = if (isOptimizationRequired) {
            Rating.entries - selection.toSet()
        } else selection
        assert(minima.size <= 1)
        val prefix = if (isOptimizationRequired) "-" else ""
        return minima.map { prefix + "rating:" + it.apiName }
    }

    override val maxLimit: Int get() = E621_MAX_POSTS_IN_QUERY

    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        return api.getPosts(tags = compileToQuery(), page = page, limit = limit).getOrThrow().posts
    }


    companion object {
        fun builder(
            options: SearchOptions? = null,
            builder: Builder.() -> Unit,
        ): PostsSearchOptions {
            return when (options) {
                null -> Builder().apply(builder).build()
                else -> Builder.from(options).apply(builder).build()
            }
        }

        fun from(options: SearchOptions) = when (options) {
            is PostsSearchOptions -> with(options) {
                PostsSearchOptions(
                    allOf = allOf.toSet(),
                    noneOf = noneOf.toSet(),
                    anyOf = anyOf.toSet(),
                    order = order,
                    orderAscending = orderAscending,
                    rating = rating.toSet(),
                    favouritesOf = favouritesOf,
                    fileType = fileType,
                    fileTypeInvert = fileTypeInvert,
                    parent = parent,
                    poolId = poolId
                )
            }

            is FavouritesSearchOptions -> PostsSearchOptions(favouritesOf = options.favouritesOf)
            is PoolSearchOptions -> PostsSearchOptions(
                poolId = options.poolId,
                order = Order.NEWEST_TO_OLDEST,
                orderAscending = true
            )
        }
    }

    class Builder(
        val allOf: MutableSet<Tag> = mutableSetOf(),
        val noneOf: MutableSet<Tag> = mutableSetOf(),
        val anyOf: MutableSet<Tag> = mutableSetOf(),
        var order: Order = Order.NEWEST_TO_OLDEST,
        var orderAscending: Boolean = false,
        var rating: MutableSet<Rating> = mutableSetOf(),
        var favouritesOf: String? = null,
        var fileType: FileType? = null,
        var fileTypeInvert: Boolean = false,
        var parent: PostId = PostId.INVALID,
        var poolId: PoolId = -1,
    ) {
        fun build() =
            PostsSearchOptions(
                allOf = allOf,
                noneOf = noneOf,
                anyOf = anyOf,
                order = order,
                orderAscending = orderAscending,
                rating = rating,
                favouritesOf = favouritesOf,
                fileType = fileType,
                fileTypeInvert = fileTypeInvert,
                parent = parent,
                poolId = poolId
            )

        companion object {
            fun from(options: SearchOptions) = when (options) {
                is PostsSearchOptions -> with(options) {
                    Builder(
                        allOf = allOf.toMutableSet(),
                        noneOf = noneOf.toMutableSet(),
                        anyOf = anyOf.toMutableSet(),
                        order = order,
                        orderAscending = orderAscending,
                        rating = rating.toMutableSet(),
                        favouritesOf = favouritesOf,
                        fileType = fileType,
                        fileTypeInvert = fileTypeInvert,
                        parent = parent,
                        poolId = poolId
                    )
                }

                is FavouritesSearchOptions -> Builder(favouritesOf = options.favouritesOf)
                is PoolSearchOptions -> Builder(
                    poolId = options.poolId,
                    order = Order.NEWEST_TO_OLDEST,
                    orderAscending = true
                )
            }
        }
    }
}