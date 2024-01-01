/*
 * This file is part of ru.herobrine1st.e621.
 *
 * ru.herobrine1st.e621 is an android client for https://e621.net
 * Copyright (C) 2022-2023 HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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


@Immutable
@Serializable
data class Sample(
    val has: Boolean, // вщ не ебу что это
    val height: Int,
    val width: Int,
    // Strange bug on API side, probably database related
    val url: String = "",
    val alternates: Map<String, Alternate>
) {
    val type by lazy {
        FileType.byExtension[url.splitToSequence(".").lastOrNull()] ?: FileType.UNDEFINED
    }
}


@Immutable
@Serializable
data class Alternate(
    val type: AlternateType,
    val height: Int,
    val width: Int,
    val urls: List<String?> // yes it really may be nullable
) {
    val normalizedType by lazy {
        urls.firstNotNullOfOrNull {
            FileType.byExtension[it?.splitToSequence(".")?.lastOrNull()]
        } ?: FileType.UNDEFINED
    }
}

@Suppress("unused")
@Serializable
enum class AlternateType {
    @SerialName("video")
    VIDEO,
    @SerialName("image")
    IMAGE
    //maybe more
}
