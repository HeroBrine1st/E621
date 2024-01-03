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

package ru.herobrine1st.e621.api.common

import androidx.compose.ui.text.AnnotatedString
import kotlinx.datetime.Instant
import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.MessageText
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.api.parseBBCode

data class CommentData(
    val id: Int,
    val author: UserData,
    val editor: UserData,
    val creationTime: Instant,
    val editTime: Instant,
    val score: Int,
    val isHidden: Boolean,
    val message: List<MessageData<*>>
) {
    companion object {
        fun fromE621Comment(comment: CommentBB, authorAvatarPost: PostReduced?) =
            CommentData(
                id = comment.id,
                author = UserData(
                    id = comment.creatorId,
                    displayName = comment.creatorName,
                    avatarUrl = authorAvatarPost?.previewUrl ?: authorAvatarPost?.croppedUrl
                ),
                editor = UserData(
                    id = comment.updaterId,
                    displayName = comment.updaterName,
                    avatarUrl = null
                ),
                creationTime = comment.createdAt,
                editTime = comment.updatedAt,
                score = comment.score,
                isHidden = comment.isHidden,
                message = parseBBCode(comment.body)
            )

        val PLACEHOLDER = CommentData(
            id = -1,
            author = UserData(
                id = -1,
                displayName = "Placeholder",
                avatarUrl = null
            ),
            editor = UserData(
                id = -1,
                displayName = "Placeholder",
                avatarUrl = null
            ),
            creationTime = Instant.DISTANT_PAST,
            editTime = Instant.DISTANT_PAST,
            score = 0,
            isHidden = false,
            message = listOf(MessageText(AnnotatedString("Placeholder\nPlaceholder")))
        )
    }

    data class UserData(
        val id: Int,
        val displayName: String,
        val avatarUrl: String?,
    )
}

