/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2025 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.herobrine1st.e621.api.TagProcessablePost
import ru.herobrine1st.e621.api.model.FileType
import ru.herobrine1st.e621.api.model.NormalizedFile
import ru.herobrine1st.e621.api.model.Post
import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Rating
import ru.herobrine1st.e621.api.model.Score
import ru.herobrine1st.e621.api.model.Tags
import ru.herobrine1st.e621.util.FavourablePost

/**
 * This class is a stripped down version of [Post] that ditches all unnecessary data on serialization
 */
@Serializable
data class TransientPost(
    override val id: PostId,
    val sample: NormalizedFile,
    override val score: Score,
    override val tags: Tags,
    override val rating: Rating,
    override val isFavourite: Boolean,
    override val commentCount: Int,
    override val favoriteCount: Int,
    val actualFileType: FileType,
    @Transient val originalPost: Post? = null,
) : TagProcessablePost, FavourablePost {
    constructor(post: Post) : this(
        post.id,
        post.normalizedSample,
        post.score,
        post.tags,
        post.rating,
        post.isFavourite,
        post.commentCount,
        post.favoriteCount,
        post.file.type,
        post
    )
}