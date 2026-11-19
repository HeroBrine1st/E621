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

import androidx.compose.runtime.Immutable
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import ru.herobrine1st.e621.api.TagProcessablePost
import ru.herobrine1st.e621.api.serializer.ISO8601Serializer
import ru.herobrine1st.e621.util.FavourablePost
import java.util.Collections.emptyList
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@JvmInline
@Serializable
value class PostId(val value: Int) {
    companion object {
        val INVALID = PostId(-1)
    }
}

@OptIn(ExperimentalTime::class)
@Immutable
@Serializable
data class Post(
    override val id: PostId,
    @Serializable(with = ISO8601Serializer::class)
    val createdAt: Instant,
    @Serializable(with = ISO8601Serializer::class)
    val updatedAt: Instant?,
    @SerialName("change_seq")
    val changeSequence: Int,
    val files: PostFiles,
    val uploaderId: Int,
    val uploaderName: String,
    val approverId: Int?,
    val stats: PostStats,
    val flags: PostFlags,
    val has: PostHas,
    val relationships: Relationships,
    val pools: List<PoolId>,
    override val rating: Rating,
    val lockedTags: List<String> = emptyList(),
    val sources: List<String>,
    val description: String,
    override val tags: Tags,
) : TagProcessablePost, FavourablePost {

    @Transient
    val normalizedFile = NormalizedFile(files)

    @Transient
    val normalizedSample = NormalizedFile("sample", files.sample) ?: normalizedFile

    @Transient
    val allFiles: List<NormalizedFile> = buildList {
        add(normalizedFile)
        normalizedSample.takeIf { it !== normalizedFile }?.let { add(it) }
        files.video?.samples?.forEach { (name, alternate) ->
            NormalizedFile(name, alternate)?.let { add(it) }
        }
    }.sortedWith(compareBy({ it.type.weight }, { it.width }))

    override val score get() = stats.score
    override val favoriteCount get() = stats.favCount
    override val commentCount get() = stats.commentCount
    override val isFavourite get() = stats.isFavourite
}

@OptIn(ExperimentalTime::class)
@Serializable
data class DeferredPost(
    val id: Int,
    val flags: String,
    val tags: String,
    val rating: Rating,
    @SerialName("file_ext")
    val type: FileType,
    val duration: Float? = null,
    val width: Int,
    val height: Int,
    val size: Int,
    @Serializable(with = ISO8601Serializer::class)
    val createdAt: Instant,
    val uploader: String,
    val uploaderId: Int,
    val score: Int,
    @SerialName("fav_count")
    val favoriteCount: Int,
    @SerialName("is_favorited")
    val isFavourite: Boolean,
    val vote: Int,
    @SerialName("comment_count")
    val commentCount: Int,
    val pools: String,  // space-separated
    val md5: String? = null,
    val previewUrl: String? = null,
    val previewWebp: String? = null,
    val sampleUrl: String? = null,
    val largeUrl: String? = null,
    val fileUrl: String? = null,
    val previewWidth: Int? = null,
    val previewHeight: Int? = null,
)
