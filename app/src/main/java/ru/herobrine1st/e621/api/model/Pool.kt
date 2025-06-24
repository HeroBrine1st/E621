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

package ru.herobrine1st.e621.api.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.api.serializer.ISO8601Serializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

typealias PoolId = Int

@OptIn(ExperimentalTime::class)
@Serializable
data class Pool(
    val id: PoolId,
    val name: String, // looks like it has underscores as spaces
    val description: String,
    @Serializable(with = ISO8601Serializer::class)
    val createdAt: Instant,
    @Serializable(with = ISO8601Serializer::class)
    val updatedAt: Instant?,
    val creatorId: Int,
    val creatorName: String,
    val isActive: Boolean,
    val category: String, // probably a boolean
    @SerialName("post_ids")
    val posts: List<PostId>,
    val postCount: Int,
) {
    val normalizedName get() = name.replace('_', ' ')
}
