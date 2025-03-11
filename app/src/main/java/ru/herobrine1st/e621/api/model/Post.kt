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
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@JvmInline
@Serializable
value class PostId(val value: Int) {
    companion object {
        val INVALID = PostId(-1)
    }
}

@Immutable
@Serializable
data class Post(
    val id: PostId,
    val createdAt: Instant,
    val updatedAt: Instant?,
    val file: File,
    val preview: Preview,
    val sample: Sample,
    val score: Score,
    val tags: Tags,
    val lockedTags: List<String> = emptyList(),
    @SerialName("change_seq")
    val changeSequence: Int,
    val flags: PostFlags,
    val rating: Rating,
    @SerialName("fav_count")
    val favoriteCount: Int,
    val sources: List<String>,
    val pools: List<PoolId>,
    val relationships: Relationships,
    val approverId: Int?,
    val uploaderId: Int,
    val description: String,
    val commentCount: Int,
    @SerialName("isFavorited")
    val isFavourite: Boolean = false,
    val hasNotes: Boolean = false,
    val duration: Float = 0f,
) {
    @Transient
    val normalizedSample = NormalizedFile(sample)

    @Transient
    val normalizedFile = NormalizedFile(file)

    @Transient
    val files: List<NormalizedFile> = listOf(
        normalizedFile,
        normalizedSample,
        *sample.alternates.filterNot { it.key == "original" }
            .map { NormalizedFile(it.key, it.value) }.toTypedArray()
    ).sortedWith(compareBy({ it.type.weight }, { it.width }))
}

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