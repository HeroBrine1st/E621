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

sealed interface UIPostListingItem {
    val contentType: String
    val key: Any

    data class Post(val post: TransientPost) : UIPostListingItem {
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

sealed interface IntermediatePostListingItem {
    val contentType: String
    val key: Any

    fun toUi(): List<UIPostListingItem>

    data class Post(val post: TransientPost) : IntermediatePostListingItem {
        override val contentType: String
            get() = "Post"
        override val key: Any
            get() = post.id.value

        override fun toUi() = listOf(UIPostListingItem.Post(post))
    }

    data class HiddenItems(
        val postIds: List<PostId>, // this list is probably a huge memory leak (~288 bits per item), but it is used to compute key for LazyList
        // those two are designed to show "connection" to post above and below, but I can't come up with design for that
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
    ) : IntermediatePostListingItem {
        fun merge(other: HiddenItems) = HiddenItems(
            postIds = this.postIds + other.postIds,
//            hasUp = this.hasUp || other.hasUp,
//            hasDown = this.hasDown || other.hasDown,
            hiddenDueToBlacklistNumber = this.hiddenDueToBlacklistNumber + other.hiddenDueToBlacklistNumber,
            hiddenDueToSafeModeNumber = this.hiddenDueToSafeModeNumber + other.hiddenDueToSafeModeNumber
        )

        override val contentType: String
            get() = "HiddenItems"
        override val key: Any
            get() = postIds

        override fun toUi(): List<UIPostListingItem> = buildList(postIds.size) {
            add(
                UIPostListingItem.HiddenItemsBridge(
                    id = postIds[0],
                    hiddenDueToBlacklistNumber = hiddenDueToBlacklistNumber,
                    hiddenDueToSafeModeNumber = hiddenDueToSafeModeNumber
                )
            )
            postIds.drop(1).map { id ->
                UIPostListingItem.Empty(id)
            }.let { list ->
                addAll(list)
            }
        }

        companion object {
            fun ofBlacklisted(post: TransientPost) = HiddenItems(
                postIds = listOf(post.id),
//                hasUp = false,
//                hasDown = false,
                hiddenDueToBlacklistNumber = 1,
                hiddenDueToSafeModeNumber = 0
            )

            fun ofUnsafe(post: TransientPost) = HiddenItems(
                postIds = listOf(post.id),
//                hasUp = false,
//                hasDown = false,
                hiddenDueToBlacklistNumber = 0,
                hiddenDueToSafeModeNumber = 1
            )
        }
    }
}

fun mergePostListingItems(
    previous: IntermediatePostListingItem,
    current: IntermediatePostListingItem,
): Pair<IntermediatePostListingItem, IntermediatePostListingItem?> {
    return when (previous) {
        is IntermediatePostListingItem.HiddenItems -> when (current) {
            is IntermediatePostListingItem.HiddenItems -> previous.merge(current) to null
            is IntermediatePostListingItem.Post -> previous/*.copy(hasDown = true)*/ to current
        }

        is IntermediatePostListingItem.Post -> when (current) {
            is IntermediatePostListingItem.HiddenItems -> previous to current/*.copy(hasUp = true)*/
            is IntermediatePostListingItem.Post -> previous to current
        }
    }
}