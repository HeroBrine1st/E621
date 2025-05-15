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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import ru.herobrine1st.e621.api.serializer.NullAsEmptyObjectSerializer


@Immutable
@Serializable
data class Sample(
    val has: Boolean, // вщ не ебу что это
    val height: Int,
    val width: Int,
    // Strange bug on API side, probably database related
    val url: String = "",
    @Serializable(with = AlternatesFieldSerializer::class)
    val alternates: Alternates? = null
) {
    @Serializable
    data class Alternates(
        val manifest: Int,
        val original: JsonElement,
        val variants: Map<String, Alternate>,
        val samples: Map<String, Alternate>
    ) {
        @Serializable
        data class Alternate(
            val width: Int,
            val height: Int,
            val url: String,
            // The defaults represent the actual values of those fields
            val fps: Int = 0,
            val size: Int = 0,
            val codec: JsonElement? = null,
        ) {
            val normalizedType get() = FileType.byExtension[url.substringAfterLast(".")] ?: FileType.UNDEFINED
        }
    }

    val type get() = FileType.byExtension[url.substringAfterLast(".")] ?: FileType.UNDEFINED
}

class AlternatesFieldSerializer : NullAsEmptyObjectSerializer<Sample.Alternates>(Sample.Alternates.serializer())
