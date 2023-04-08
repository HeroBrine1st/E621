package ru.herobrine1st.e621.ui.screen.post.data

import ru.herobrine1st.e621.api.MessageData
import ru.herobrine1st.e621.api.model.CommentBB
import ru.herobrine1st.e621.api.model.PostReduced
import ru.herobrine1st.e621.api.parseBBCode
import java.time.OffsetDateTime

// This package may be not the best for this classes
// It is still WIP, as I don't know ahead of time how will I replace E621 models with my own
// At the end, I will be able to introduce more APIs to this application

data class CommentData(
    val id: Int,
    val author: UserData,
    val editor: UserData,
    val creationTime: OffsetDateTime,
    val editTime: OffsetDateTime,
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
    }

    data class UserData(
        val id: Int,
        val displayName: String,
        val avatarUrl: String?,
    )
}

