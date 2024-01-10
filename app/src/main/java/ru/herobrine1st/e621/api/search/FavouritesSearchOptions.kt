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

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonPrimitive
import ru.herobrine1st.e621.api.API
import ru.herobrine1st.e621.api.E621_MAX_POSTS_IN_QUERY
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.util.InternalState

@Serializable
data class FavouritesSearchOptions(
    val favouritesOf: String,
    @set:InternalState var id: Int? = null,
) : SearchOptions {
    override val maxLimit: Int get() = E621_MAX_POSTS_IN_QUERY

    @OptIn(InternalState::class)
    override suspend fun getPosts(api: API, limit: Int, page: Int): List<Post> {
        id = id ?: favouritesOf.let {
            api.getUser(favouritesOf).getOrThrow()["id"]!!.jsonPrimitive.content.toInt()
        }
        return api.getFavourites(userId = id, page = page, limit = limit).getOrThrow().posts
    }
}