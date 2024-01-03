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

package ru.herobrine1st.e621.api.endpoint.posts

import androidx.annotation.IntRange
import io.ktor.resources.Resource
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.api.HttpMethod
import ru.herobrine1st.e621.api.HttpMethodType
import ru.herobrine1st.e621.api.endpoint.APIEndpoint
import ru.herobrine1st.e621.api.model.PostId

@Serializable
@HttpMethod(HttpMethodType.POST)
@Resource("/posts/{id}/votes.json")
data class VoteEndpoint(
    val id: PostId,
    @IntRange(from = -1, to = 1) val score: Int,
    @Suppress("SpellCheckingInspection")
    @SerialName("no_unvote") val noRetractVote: Boolean,
): APIEndpoint<Unit, VoteEndpoint.Response> {
    @Serializable
    data class Response(val up: Int, val down: Int, val total: Int, val ourScore: Int)
}