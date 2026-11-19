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

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonIgnoreUnknownKeys
import java.util.Collections.emptyMap

@Serializable
data class PostFiles(
    val meta: FileMeta,
    val original: FileOriginal,
    val preview: FileVariants,
    val sample: FileVariants,
    val video: FileVideo? = null,
)

@Serializable
data class FileMeta(
    val md5: String,
    @SerialName("ext")
    val type: FileType,
    val size: Long,
    val duration: Float? = null,
    val hasSample: Boolean = false,
)

@Serializable
data class FileOriginal(
    val width: Int,
    val height: Int,
    // URL may be null - strange bug on API side, probably database related
    val url: String?,
)

@Serializable
data class FileVariants(
    val width: Int,
    val height: Int,
    val jpg: String? = null,
    val webp: String? = null,
) {
    val urls get() = listOfNotNull(jpg, webp)
}

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class FileVideo(
    val has: Boolean = false,
    val original: VideoAlternate? = null,
    val variants: FileVideoVariants? = null,
    val samples: Map<String, VideoAlternate> = emptyMap(),
)

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class FileVideoVariants(
    val webm: VideoAlternate? = null,
    val mp4: VideoAlternate? = null,
)

@Serializable
@JsonIgnoreUnknownKeys
@OptIn(ExperimentalSerializationApi::class)
data class VideoAlternate(
    val width: Int,
    val height: Int,
    val url: String,
    val fps: Float = 0f,
    val size: Long = 0L,
    val codec: String? = null,
) {
    val type get() = FileType.byExtension[url.substringAfterLast(".")]
}

@Serializable
data class NormalizedFile(
    val name: String,
    val width: Int,
    val height: Int,
    val type: FileType,
    val size: Long,
    val urls: List<String>
) {
    constructor(name: String, width: Int, height: Int, type: FileType, size: Long, url: String) :
            this(name, width, height, type, size, listOf(url))

    val aspectRatio get() = width.toFloat() / height.toFloat()

    val simpleType get() = type.simpleType

    companion object {
        operator fun invoke(files: PostFiles): NormalizedFile {
            // if there's no url so be it. There's no infrastructure to handle absence of ANY file
            return NormalizedFile(
                "original",
                files.original.width,
                files.original.height,
                files.meta.type,
                files.meta.size,
                listOfNotNull(files.original.url),
            )
        }

        operator fun invoke(name: String, variants: FileVariants): NormalizedFile? {
            val type = variants.urls.firstOrNull()
                ?.let { FileType.byExtension[it.substringAfterLast(".")] } ?: return null
            return NormalizedFile(name, variants.width, variants.height, type, 0, variants.urls)
        }

        operator fun invoke(name: String, alternate: VideoAlternate): NormalizedFile? {
            return NormalizedFile(
                name,
                alternate.width,
                alternate.height,
                alternate.type ?: return null,
                alternate.size,
                alternate.url,
            )
        }
    }
}

fun NormalizedFile.isOriginal() = name == "original"
