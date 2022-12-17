/*
 * ru.herobrine1st.e621 is an android client for https://e621.net and https://e926.net
 * Copyright (C) 2022  HeroBrine1st Erquilenne <project-e621-android@herobrine1st.ru>
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

import android.os.Parcelable
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
data class File(
    val width: Int,
    val height: Int,
    @JsonProperty("ext")
    val type: FileType,
    val size: Long,
    val md5: String,
    val url: String
) : Parcelable

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

    constructor(file: File) :
            this("original", file.width, file.height, file.type, file.size, file.url)

    constructor(file: Sample) :
            this("sample", file.width, file.height, file.type, 0, file.url)

    constructor(name: String, file: Alternate) :
            this(name, file.width, file.height, file.normalizedType, 0, file.urls.filterNotNull())

    @get:JsonIgnore
    @IgnoredOnParcel
    val aspectRatio get() = width.toFloat() / height.toFloat()
}