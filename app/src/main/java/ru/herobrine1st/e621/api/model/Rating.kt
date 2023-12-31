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

import androidx.annotation.StringRes
import com.fasterxml.jackson.annotation.JsonValue
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ru.herobrine1st.e621.R

@Serializable
enum class Rating(@StringRes val descriptionId: Int, val apiName: String) {
    // STOPSHIP: SerialNames are inferred according to @JsonValue below and aren't verified yet
    @SerialName("s") SAFE(R.string.rating_safe, "safe"),
    @SerialName("q") QUESTIONABLE(R.string.rating_questionable, "questionable"),
    @SerialName("e") EXPLICIT(R.string.rating_explicit, "explicit");

    @JsonValue
    val shortName = apiName.substring(0, 1)

    inline val isNotSafe get() = this != SAFE

    companion object {
        val byAnyName: Map<String, Rating> = HashMap<String, Rating>().apply {
            Rating.entries.forEach {
                put(it.apiName.lowercase(), it) // lowercase just in case
                put(it.shortName.lowercase(), it)
            }
        }
    }
}
