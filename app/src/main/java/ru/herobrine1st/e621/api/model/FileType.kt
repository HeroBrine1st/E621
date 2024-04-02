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
import okhttp3.internal.toImmutableMap

@Serializable
enum class FileType(
    val extension: String,
    val isSupported: Boolean = true,
    val isImage: Boolean = false,
    val isVideo: Boolean = false,
    val weight: Byte = 0 // to sort by sample type and then by resolution
) {
    @SerialName("jpg")
    JPG("jpg", isImage = true),
    @SerialName("png")
    PNG("png", isImage = true),
    @SerialName("gif")
    GIF("gif", isImage = true, weight = 1),
    @SerialName("swf")
    SWF("swf", isSupported = false),
    @SerialName("webm")
    WEBM("webm", isVideo = true, weight = 2),
    UNDEFINED("undefined", isSupported = false);

    val isNotImage = !isImage

    companion object {
        val byExtension = mutableMapOf<String, FileType>().apply {
            FileType.entries.forEach { this[it.extension] = it }
        }.toImmutableMap()

        fun supportedValues() = entries.filter { it.isSupported }
    }
}

