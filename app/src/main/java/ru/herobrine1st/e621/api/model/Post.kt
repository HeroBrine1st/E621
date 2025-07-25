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
    val file: File,
    val preview: Preview,
    val sample: Sample,
    override val score: Score,
    override val tags: Tags,
    val lockedTags: List<String> = emptyList(),
    @SerialName("change_seq")
    val changeSequence: Int,
    val flags: PostFlags,
    override val rating: Rating,
    @SerialName("fav_count")
    override val favoriteCount: Int,
    val sources: List<String>,
    val pools: List<PoolId>,
    val relationships: Relationships,
    val approverId: Int?,
    val uploaderId: Int,
    val description: String,
    override val commentCount: Int,
    @SerialName("isFavorited")
    override val isFavourite: Boolean = false,
    val hasNotes: Boolean = false,
    val duration: Float = 0f,
) : TagProcessablePost, FavourablePost {
    @Transient
    val normalizedSample = NormalizedFile(sample)

    @Transient
    val normalizedFile = NormalizedFile(file)

    @Transient
    val files: List<NormalizedFile> = buildList {
        add(normalizedFile)
        add(normalizedSample)
        sample.alternates?.let { alternates ->
            addAll(alternates.samples.map { NormalizedFile(it.key, it.value) })
        }
    }.sortedWith(compareBy({ it.type.weight }, { it.width }))
}

@OptIn(ExperimentalTime::class)
@Serializable
data class PostReduced(
    val id: Int,
    val flags: String,
    val tags: String,
    val rating: Rating,
    @SerialName("file_ext")
    val type: FileType,
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
    val pools: List<PoolId>,
    val md5: String?,
    val previewUrl: String,
    val largeUrl: String,
    val fileUrl: String,
    val previewWidth: Int,
    val previewHeight: Int
)

fun Post.selectSample() = when {
    file.type.isVideo -> files.first { it.type.isVideo }
    else -> normalizedSample
}