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
import org.jsoup.Jsoup
import ru.herobrine1st.e621.api.endpoint.posts.GetPostCommentsHTMLEndpoint
import ru.herobrine1st.e621.api.serializer.ISO8601Serializer
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Serializable
data class CommentBB(
    val id: Int,
    @Serializable(with = ISO8601Serializer::class)
    val createdAt: Instant,
    @SerialName("post_id")
    val parentPostId: PostId,
    val creatorId: Int,
    val body: String,
    val score: Int,
    @Serializable(with = ISO8601Serializer::class)
    val updatedAt: Instant, // same aas createdAt if not updated
    val updaterId: Int, // same as creatorId if not updated
    @Deprecated("API-deprecated")
    val doNotBumpPost: Boolean = false,
    val isHidden: Boolean, // okay maybe I understand it
    val isSticky: Boolean, // wtf
    val warningType: WarningType?, // warning, record, ban
    val warningUserId: Int?,
    val creatorName: String,
    val updaterName: String
)

fun parseCommentAvatarsAndGetCommentCount(response: GetPostCommentsHTMLEndpoint.Response): Pair<Map<Int, PostReduced?>, Int> {
    val document = Jsoup.parse(response.html).body()
    return document.children().associate { article ->
        val id = article.attr("data-comment-id").toInt()
        val avatarPostId: Int =
            article.getElementsByClass("author-info").first()!!.let { authorInfo ->
                authorInfo.getElementsByClass("avatar").first()
                    ?.getElementsByClass("post-thumb")?.first()?.attr("data-id")
                    ?.toIntOrNull() ?: 0
            }
        id to response.posts[avatarPostId]
    } to document.children().size
}