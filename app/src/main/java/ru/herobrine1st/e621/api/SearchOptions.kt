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

package ru.herobrine1st.e621.api

import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import androidx.navigation.NavType
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.Order
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.util.JsonSerializable
import ru.herobrine1st.e621.util.debug
import ru.herobrine1st.e621.util.getParcelableCompat
import ru.herobrine1st.e621.util.objectMapper
import java.io.IOException

interface SearchOptions {
    @Throws(ApiException::class, IOException::class)
    suspend fun getPosts(api: API, limit: Int, page: Int): List<Post>

    fun toBuilder(builder: PostsSearchOptions.Builder.() -> Unit) =
        PostsSearchOptions.builder(this, builder)
}

@Parcelize
data class PostsSearchOptions(
    val tags: List<String> = emptyList(),
    val order: Order = Order.NEWEST_TO_OLDEST,
    val orderAscending: Boolean = false,
    val rating: List<Rating> = emptyList(),
    val favouritesOf: String? = null, // "favorited_by" in api
    val fileType: FileType? = null,
    val fileTypeInvert: Boolean = false
) : SearchOptions, Parcelable, JsonSerializable {
    // TODO randomSeed or something like that for Order.RANDOM

    private fun compileToQuery(): String {
        val cache = mutableListOf<String>()

        cache += tags
        cache += optimizeRatingSelection(rating)
        fileType?.extension?.let { cache += (if (fileTypeInvert) "-" else "") + "type:" + it }
        favouritesOf?.let { cache += "fav:$it" }
        (if (orderAscending) order.ascendingApiName else order.apiName)?.let { cache += "order:$it" }

        return cache.joinToString(" ").debug {
            Log.d(PostsSearchOptions::class.simpleName, "Built query: $this")
        }
    }

    private fun optimizeRatingSelection(
        selection: Collection<Rating>,
    ): List<String> {
        val isOptimizationRequired = selection.size > Rating.values().size / 2
        val minima = if (isOptimizationRequired) {
            Rating.values().toList() - selection.toSet()
        } else selection
        assert(minima.size <= 1)
        val prefix = if (isOptimizationRequired) "-" else ""
        return minima.map { prefix + "rating:" + it.apiName }
    }

    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        return api.getPosts(tags = compileToQuery(), page = page, limit = limit).await().posts
    }

    companion object {
        val DEFAULT =
            PostsSearchOptions(
                emptyList(),
                Order.NEWEST_TO_OLDEST,
                false,
                emptyList(),
                null,
                null,
                false
            )

        fun builder(
            options: SearchOptions? = null,
            builder: Builder.() -> Unit
        ): PostsSearchOptions {
            return when (options) {
                null -> Builder().apply(builder).build()
                else -> Builder.from(options).apply(builder).build()
            }
        }
    }

    class Builder(
        var tags: List<String> = mutableListOf(),
        var order: Order = Order.NEWEST_TO_OLDEST,
        var orderAscending: Boolean = false,
        var rating: List<Rating> = mutableListOf(),
        var favouritesOf: String? = null,
        var fileType: FileType? = null,
        var fileTypeInvert: Boolean = false
    ) {
        fun build() =
            PostsSearchOptions(tags, order, orderAscending, rating, favouritesOf, fileType, fileTypeInvert)

        companion object {
            fun from(options: SearchOptions) = when (options) {
                is PostsSearchOptions -> with(options) {
                    Builder(tags, order, orderAscending, rating, favouritesOf)
                }
                is FavouritesSearchOptions -> Builder(favouritesOf = options.favouritesOf)
                else -> throw NotImplementedError()
            }
        }
    }
}

data class FavouritesSearchOptions(val favouritesOf: String?) : SearchOptions {
    private var id: Int? = null
    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        id = id ?: favouritesOf?.let {
            api.getUser(favouritesOf).await().get("id").asInt()
        }
        return api.getFavourites(userId = id, page = page, limit = limit).await().posts
    }
}

class PostsSearchOptionsNavType : NavType<PostsSearchOptions?>(true) {
    override fun get(bundle: Bundle, key: String): PostsSearchOptions? {
        return bundle.getParcelableCompat(key)
    }

    override fun parseValue(value: String): PostsSearchOptions {
        return objectMapper.readValue(value)
    }

    override fun put(bundle: Bundle, key: String, value: PostsSearchOptions?) {
        bundle.putParcelable(key, value)
    }
}