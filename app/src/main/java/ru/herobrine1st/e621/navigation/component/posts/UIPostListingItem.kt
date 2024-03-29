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

package ru.herobrine1st.e621.navigation.component.posts

import ru.herobrine1st.e621.api.model.PostId
import ru.herobrine1st.e621.api.model.Post as ModelPost

sealed interface UIPostListingItem {
    val contentType: String
    val key: Any

    data class Post(val post: ModelPost) : UIPostListingItem {
        override val contentType: String
            get() = "Post"
        override val key: Any
            get() = post.id.value
    }


    // The logic in Empty and HiddenItemsBridge is that a single bridge and then series of Empty
    // are generated from one HiddenItems
    // This behavior will allow LazyList to restore position under (hopefully) any circumstances
    data class Empty(val id: PostId) : UIPostListingItem {
        override val contentType: String
            get() = "Empty"

        override val key: Any
            get() = id.value

    }

    data class HiddenItemsBridge(
        val id: PostId,
        /**
         * True if there's a shown post above
         */
//        val hasUp: Boolean,
        /**
         * True if there's a shown post below
         */
//        val hasDown: Boolean,
        val hiddenDueToBlacklistNumber: Int,
        val hiddenDueToSafeModeNumber: Int,
    ) : UIPostListingItem {

        override val contentType: String
            get() = "HiddenItemsBridge"

        override val key: Any
            get() = id.value
    }
}