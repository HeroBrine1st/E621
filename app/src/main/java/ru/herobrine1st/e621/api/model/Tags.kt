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
import androidx.compose.runtime.Immutable
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

@Parcelize
@Immutable
@JsonIgnoreProperties("invalid", "all", "reduced")
data class Tags(
    val general: List<String>,
    val species: List<String>,
    val character: List<String>,
    val copyright: List<String>,
    val artist: List<String>,
    val lore: List<String>,
    val meta: List<String>
) : Parcelable {
    @IgnoredOnParcel
    val all by lazy {
        artist + copyright + character + species + general + lore + meta
    }
    @IgnoredOnParcel
    val reduced by lazy {
        copyright + artist + character
    }
}