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

package ru.herobrine1st.e621.navigation.config

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import ru.herobrine1st.e621.api.PostsSearchOptions
import ru.herobrine1st.e621.api.SearchOptions
import ru.herobrine1st.e621.api.model.Tag
import ru.herobrine1st.e621.api.model.Post as ModelPost

// weak TO/DO: data objects https://youtrack.jetbrains.com/issue/KT-40218
// Compose/Android is not affected tho
@Parcelize
sealed interface Config : Parcelable {
    // Can only be the first in stack
    @Parcelize
    object Home : Config

    // Index is used to distinguish otherwise equal configurations
    @Parcelize
    data class Search(
        val initialSearch: PostsSearchOptions = PostsSearchOptions(),
        private val index: Int
    ) : Config

    @Parcelize
    data class PostListing(
        val search: SearchOptions,
        private val index: Int
    ) : Config

    @Parcelize
    data class Post(
        val id: Int,
        val post: ModelPost?,
        val openComments: Boolean = false,
        val query: SearchOptions,
        private val index: Int
    ) : Config

    @Parcelize
    data class Wiki(
        val tag: Tag,
        private val index: Int
    ) : Config

    // Not needed: already covered by [PostListing]
    // data class Favourites

    // These are used only once in a stack
    @Parcelize
    object Settings : Config {
        @Parcelize
        object Blacklist : Config {
            @Parcelize
            data class Entry(
                val id: Long,
                val query: String,
                val enabled: Boolean
            ) : Config
        }

        @Parcelize
        object About : Config

        @Parcelize
        object License : Config

        @Parcelize
        object AboutLibraries : Config
    }
}