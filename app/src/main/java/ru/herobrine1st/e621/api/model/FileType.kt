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

import kotlinx.collections.immutable.toImmutableMap
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.api.model.SimpleFileType.*

// A simplification of filetype, as there's actually no need to differ between png and jpg
enum class SimpleFileType {
    IMAGE,
    ANIMATION,
    VIDEO
}

@Serializable
enum class FileType(
    val extension: String,
    val simpleType: SimpleFileType,
    val isSupported: Boolean = true,
    val weight: Byte = 0 // to sort by sample type and then by resolution
) {
    @SerialName("jpg")
    JPG("jpg", IMAGE),
    @SerialName("png")
    PNG("png", IMAGE),

    @SerialName("webp")
    WEBP("webp", IMAGE),
    @SerialName("gif")
    GIF("gif", ANIMATION, weight = 1),
    @SerialName("swf")
    SWF("swf", ANIMATION, isSupported = false),
    @SerialName("webm")
    WEBM("webm", VIDEO, weight = 2),
    @SerialName("mp4")
    MP4("mp4", VIDEO, weight = 2);

    companion object {
        inline val supportedEntries get() = entries.filter { it.isSupported }
        val byExtension = mutableMapOf<String, FileType>().apply {
            FileType.entries.forEach { this[it.extension] = it }
        }.toImmutableMap()
    }
}

